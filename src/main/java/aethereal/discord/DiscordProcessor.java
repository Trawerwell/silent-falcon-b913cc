package aethereal.discord;


import aethereal.config.BaseProcessor;
import aethereal.core.Primordial;

import java.io.IOException;

public class DiscordProcessor extends BaseProcessor {
    private DiscordIPC b;

    @Override

    public void setup() {
    }

    @Override
    public void unSetup() {
    }

    public DiscordIPC a() {
        return this.b;
    }

    public void a(Void result, Throwable ex) {
        if (ex == null) {
            try {
                this.b.a(new Activity.a().type(ActivityType.PLAYING).b("username: " + Primordial.getInstance().g().username()).state("build: " + (Primordial.getInstance().c() != null ? "development" : "public")).largeImage("https://deltaclient.xyz/api/logotype.png", "https://deltaclient.xyz/").startAt(System.currentTimeMillis() / 1000).largeImage("https://i.imgur.com/E6dkFRc.jpeg", "https://deltaclient.xyz/").c("Купить", "https://deltaclient.xyz/").c("Новости", "https://t.me/collapseloader").build());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
