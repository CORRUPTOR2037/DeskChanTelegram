import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import info.deskchan.core.*;

public class Main implements Plugin {
    private static PluginProxyInterface pluginProxy;

    public static void main(String[] args){

    }
    @Override
    public boolean initialize(PluginProxyInterface newPluginProxy) {
        pluginProxy = newPluginProxy;
        log("loading api");
        try {
            Properties properties = new Properties();
            try {
                InputStream ip = Files.newInputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
                properties.load(ip);
                ip.close();
            } catch (Exception e) {
                properties = null;
                log("Cannot find file: " + pluginProxy.getDataDirPath().resolve("config.properties"));
                //log(e);
            }
            if (properties != null) {
                try {
                    App.token = properties.getProperty("token");
                } catch (Exception e) {
                }
            }
        } catch(Exception e){ pluginProxy.log(e); }
        pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("start"));
            put("msgTag", "telegram:start");
        }});
        pluginProxy.addMessageListener("telegram:start", (sender, tag, data) -> {
            App.Start();
        });
        pluginProxy.addMessageListener("telegram:options-saved",(sender, tag, da) -> {
            Map<String, Object> data = (Map<String, Object>) da;
            if(data.containsKey("token"))
                App.token=(String)data.get("token");
            if(data.containsKey("id"))
                App.masterId=Integer.parseInt((String) data.get("id"));
            updateMenu();
        });
        pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("stop"));
            put("msgTag", "telegram:stop");
        }});
        pluginProxy.addMessageListener("telegram:stop", (sender, tag, data) -> {
            App.Stop();
        });
        pluginProxy.sendMessage("talk:add-reciever",new HashMap<String, Object>() {{
            put("tag", "telegram:send");
        }});
        pluginProxy.addMessageListener("telegram:send", (sender, tag, data) -> {
            Map<String, Object> da = (Map<String, Object>) data;
            if(da.containsKey("text"))
                App.Send((String)da.get("text"));
            else if(da.containsKey("value"))
                App.Send((String)da.get("value"));
        });
        updateMenu();
        log("api loaded");
        return true;
    }

    void updateMenu(){
        pluginProxy.sendMessage("gui:setup-options-tab", new HashMap<String, Object>() {{
            put("name", "Telegram" );
            put("msgTag", "telegram:options-saved");
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "token");
                put("type", "TextField");
                put("label", "Token");
                put("value", App.token);
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "id");
                put("type", "TextField");
                put("label", "Id");
                put("value", App.masterId);
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "text");
                put("type", "TextField");
                put("label", "Send message");
                put("enterTag","telegram:send");
                put("value", "");
            }});
            put("controls", list);
        }});
    }
    static String getString(String text){
        return getPluginProxy().getString(text);
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }

    public static void sendToProxy(String tag, Map<String, Object> data) {
        pluginProxy.sendMessage(tag, data);
    }

    public static Path getDataDirPath() {
        return pluginProxy.getDataDirPath();
    }

    static PluginProxyInterface getPluginProxy() { return pluginProxy; }

    public void unload(){
        Properties properties = new Properties();
        properties.setProperty("token", App.token);
        try {
            OutputStream ip = Files.newOutputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
            properties.store(ip, "config fot telegram api");
            ip.close();
        } catch (IOException e) {
            log(e);
        }
    }
}