package org.novasparkle.sateproxy.auth;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.Setter;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.novasparkle.sateproxy.SateProxy;
import org.novasparkle.sateproxy.auth.sql.AuthSqlManager;
import org.novasparkle.sateproxy.configuration.ConfigManager;
import org.novasparkle.sateproxy.utilities.Utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ActiveLoginSession implements LimboSessionHandler {

    private final VirtualWorld virtualWorld;
    private LimboPlayer limboPlayer;
    @Getter @Setter
    private AtomicInteger authAttemptsLeft;
    private AtomicInteger authTimeLeft;
    private final List<ScheduledTask> tasks;

    public ActiveLoginSession(VirtualWorld virtualWorld) {
        this.tasks = new ArrayList<>();
        this.virtualWorld = virtualWorld;
        this.authAttemptsLeft = new AtomicInteger(ConfigManager.getInt("auth.attempts"));
    }


    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.limboPlayer = player;
        Player proxyPlayer = limboPlayer.getProxyPlayer();

        if (AuthSqlManager.isRegistered(proxyPlayer)) {
            this.startInfoTask(proxyPlayer, "login");
            this.authTimeLeft = new AtomicInteger(ConfigManager.getInt("auth.loginTimeout"));
        } else {
            this.startInfoTask(proxyPlayer, "register");
            this.authTimeLeft = new AtomicInteger(ConfigManager.getInt("auth.registerTimeout"));
        }
    }

    @Override
    public void onDisconnect() {
        this.cancelAll();
    }

    private void startTimeTask(Player player) {
        TimeUnit unit = TimeUnit.SECONDS;
        SateProxy instance = SateProxy.instance;

        String actionBarMessage = ConfigManager.getMessage("timeLeft").get(0);
        AtomicInteger copyTimeOut = new AtomicInteger(authTimeLeft.get());

        ScheduledTask actionBarTask = instance.getScheduler()
                .buildTask(instance, () -> {
                    String editedMessage = Utils.applyReplacements(actionBarMessage, "time-%-" + authTimeLeft);
                    player.sendActionBar(Utils.toComponent(editedMessage));
                    authTimeLeft.decrementAndGet();
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .repeat(1, unit)
                .schedule();

        ScheduledTask kickTask = instance.getScheduler()
                .buildTask(instance, () -> {
                    player.disconnect(ConfigManager.getMessageAsComponent("timeout").get(0));
                })
                .delay(copyTimeOut.get(), unit)
                .schedule();


        this.registerTask(kickTask, actionBarTask);
    }

    private void startInfoTask(Player player, String path) {
        int repeatValue = ConfigManager.getInt(String.format("messages.%s.title.repeatEvery", path));
        TimeUnit unit = TimeUnit.valueOf(ConfigManager.getMessage(path + ".title.timeUnit").get(0));

        ScheduledTask task = SateProxy.instance.getScheduler()
                .buildTask(SateProxy.instance, () -> this.showInfo(player, path))
                .repeat(repeatValue, unit)
                .schedule();

        this.registerTask(task);
    }

    private void showInfo(Player player, String path) {
        Component title = ConfigManager.getMessageAsComponent(path + ".title.title").get(0);
        Component subtitle = ConfigManager.getMessageAsComponent(path + ".title.subtitle").get(0);

        Duration in = Ticks.duration(ConfigManager.getLong(String.format("messages.%s.title.in", path)));
        Duration stay = Ticks.duration(ConfigManager.getLong(String.format("messages.%s.title.stay", path)));
        Duration out = Ticks.duration(ConfigManager.getLong(String.format("messages.%s.title.out", path)));

        player.showTitle(Title.title(title, subtitle, Title.Times.times(in, stay, out)));
        ConfigManager.sendMessage(player, path + ".info", "attempts-%-" + authAttemptsLeft.get());
    }

    private void registerTask(ScheduledTask... taskList) {
        tasks.addAll(Arrays.asList(taskList));
    }

    private void cancelTask(ScheduledTask task) {
        task.cancel();
        tasks.remove(task);
    }

    public void cancelAll() {
        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }
}
