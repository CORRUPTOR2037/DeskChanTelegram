
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramBotAdapter;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.pengrad.telegrambot.response.SendResponse;

import info.deskchan.core.*;

import java.util.HashMap;
import java.util.List;

class App {
    private static class ChatHandler implements ResponseListener{
        private Object lastSeq = null;
        @Override
        public void handle(String sender, Object data) {
            try {
                GetUpdatesResponse response = bot.execute(getUpdates);
                List<Update> updates = response.updates();
                for (Update update : updates) {
                    //System.out.println(update.message());
                    if(update.message()==null) continue;
                    if (!lastUpdates.containsKey(update.message().chat()))
                        lastUpdates.put(update.message().chat(),0);
                    if (lastUpdates.get(update.message().chat()) >= update.updateId()) continue;
                    lastUpdates.replace(update.message().chat(),update.updateId());
                    if (update.message()==null || update.message().text()==null) continue;
                    if(!skipHistory) continue;
                    App.AnalyzeMessage(update.message());
                }
                if(!skipHistory) skipHistory=true;
            } catch (Exception e){
                Main.log(e);
            }
            lastSeq = null;
            start();
        }
        void start() {
            if (lastSeq != null)
                stop();
            HashMap<String,Object> map=new HashMap<String, Object>();
            map.put("delay", 2000);
            lastSeq = Main.getPluginProxy().sendMessage("core-utils:notify-after-delay",  map, this);
        }
        void stop() {
            if (lastSeq != null)
                Main.getPluginProxy().sendMessage("core-utils:notify-after-delay", new HashMap<String, Object>() {{
                    put("seq", lastSeq);
                    put("delay", (long) -1);
                }});
        }
    }
    private static boolean skipHistory;
    private static TelegramBot bot;
    private static GetUpdates getUpdates;
    private static HashMap<Chat,Integer> lastUpdates;
    private static Chat currentChat=null;
    private static String selfName="@five_nine_one_bot";
    public static Integer masterId=null;
    public static String token=null;
    private static ChatHandler handler=new ChatHandler();
    static void AnalyzeMessage(Message message){
        String text=message.text();
        text=text.replace(selfName,"");
        System.out.println(text);
        String[] words=text.split(" ");
        if(words[0].equals("/notice")){
            if(message.from().id().equals(masterId)){
                currentChat=message.chat();
                bot.execute(new GetChat(currentChat));
                if(!lastUpdates.containsKey(currentChat)) {
                    skipHistory = false;
                    lastUpdates.put(currentChat, 0);
                }
                SendTalkRequest("HELLO");
                System.out.println("all okay");
            } else SendTalkRequest("REFUSE");
            return;
        }
        //SendTalkRequest("CHAT");
    }
    static void Send(String text){
        System.out.println(currentChat);
        if(currentChat==null) return;
        SendMessage request = new SendMessage(currentChat.id(), text);
        request.parseMode(ParseMode.HTML);
        request.disableWebPagePreview(true);

        SendResponse sendResponse = bot.execute(request);
        boolean ok = sendResponse.isOk();
        Message message = sendResponse.message();
        //System.out.println(ok+" "+message);
    }
    static void SendTalkRequest(String type){
        Main.getPluginProxy().sendMessage("talk:request",new HashMap<String, Object>() {{
            put("purpose", type);
        }});
    }
    static void Start(){
        if(token==null || masterId==null){
            Main.getPluginProxy().sendMessage("gui:show-notification",new HashMap<String, Object>() {{
                put("text", "Token or Master ID not specified");
            }});
            return;
        }
        skipHistory=false;
        lastUpdates=new HashMap<Chat,Integer>();
        System.out.println(lastUpdates);
        bot = TelegramBotAdapter.build(token);
        getUpdates = new GetUpdates().limit(100).offset(0).timeout(1000);

        handler.start();
        GetChat chatRequest=new GetChat(masterId);
        GetChatResponse resp=bot.execute(chatRequest);
        currentChat=resp.chat();

        System.out.println(currentChat);

        lastUpdates.put(currentChat,0);

        SendTalkRequest("HELLO");
    }
    static void Stop(){
        handler.stop();
    }
}
