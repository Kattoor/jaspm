package monitor;

import com.moandjiezana.toml.Toml;

import java.io.InputStream;

public class Config {

    private final Toml configToml;
    private final Toml configSecretToml;

    private Config() {
        InputStream configFile = this.getClass().getClassLoader().getResourceAsStream("config.toml");
        InputStream configSecretFile = this.getClass().getClassLoader().getResourceAsStream("config.secret.toml");
        configToml = new Toml().read(configFile);
        configSecretToml = new Toml().read(configSecretFile);
    }

    private static Config instance;

    private static Config getInstance() {
        if (instance == null)
            instance = new Config();
        return instance;
    }

    public static Toml get() {
        return getInstance().configToml;
    }

    public static Toml getSecret() {
        return getInstance().configSecretToml;
    }
}
