import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.sun.org.apache.xerces.internal.xs.StringList;
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
                    App.token = properties.getProperty("token","");
                    App.masterId = Integer.parseInt(properties.getProperty("masterId",""));
                } catch (Exception e) {
                }
            }
        } catch(Exception e){ pluginProxy.log(e); }
        ArrayList<HashMap<String,Object>> actions = new ArrayList<HashMap<String,Object>>();
        actions.add(new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("start"));
            put("msgTag", "telegram:start");
        }});
        actions.add(new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("stop"));
            put("msgTag", "telegram:stop");
        }});
        pluginProxy.sendMessage("DeskChan:register-simple-actions", actions);
        pluginProxy.addMessageListener("telegram:start", (sender, tag, data) -> {
            pluginProxy.sendMessage("core:change-alternative-priority",new HashMap<String, Object>() {{
                put("srcTag", "DeskChan:say");
                put("dstTag", "telegram:send");
                put("priority", 500);
            }});
            App.Start();
        });
        pluginProxy.addMessageListener("telegram:options-saved",(sender, tag, da) -> {
            Map<String, Object> data = (Map<String, Object>) da;
            if(data.containsKey("token"))
                App.token=(String)data.get("token");
            if(data.containsKey("id"))
                App.masterId=Integer.parseInt((String) data.get("id"));
            updateMenu();
            saveSettings();
        });
        pluginProxy.addMessageListener("telegram:stop", (sender, tag, data) -> {
            App.Stop();
            pluginProxy.sendMessage("core:change-alternative-priority",new HashMap<String, Object>() {{
                put("srcTag", "DeskChan:say");
                put("dstTag", "telegram:send");
                put("priority", 50);
            }});
        });
        pluginProxy.sendMessage("core:register-alternative",new HashMap<String, Object>() {{
            put("srcTag", "DeskChan:say");
            put("dstTag", "telegram:send");
            put("priority", 50);
        }});
        pluginProxy.addMessageListener("telegram:send", (sender, tag, data) -> {
            if (data instanceof String) {
                App.Send((String) data);
                return;
            }
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
        pluginProxy.sendMessage("gui:setup-options-submenu", new HashMap<String, Object>() {{
            put("name", getString("options") );
            put("msgTag", "telegram:options-saved");
            List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "token");
                put("type", "TextField");
                put("label", "Token");
                put("hint", Main.getString("info.token"));
                put("value", App.token);
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "id");
                put("type", "TextField");
                put("label", "ID");
                put("hint", Main.getString("info.id"));
                put("value", App.masterId);
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "text");
                put("type", "TextField");
                put("label", Main.getString("send"));
                put("enterTag","telegram:send");
                put("hint", Main.getString("info.send"));
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

    public void saveSettings(){
        Properties properties = new Properties();
        if(App.token!=null) properties.setProperty("token", App.token);
        if(App.masterId!=null) properties.setProperty("masterId", App.masterId.toString());
        try {
            OutputStream ip = Files.newOutputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
            properties.store(ip, "config fot telegram api");
            ip.close();
        } catch (IOException e) {
            log(e);
        }
    }
    public void unload(){
        App.Stop();
        saveSettings();
    }
}