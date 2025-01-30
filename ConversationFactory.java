package mts;

// am separat logica asignarii tipului de thread utilizat, care initial era in functia main
// strict pentru ca functia main (Server) devenise prea stufoasa si devenise dificil de monitorizat ce si unde se intampla
// similar si pentru clasa MessageUtils, deoarece exista duplicat in ambele clase de thread-uri in versiunile anterioare

public class ConversationFactory {
    public static Runnable createConversation(int conversationId, Server ui) {
        // Alterneaza intre cele doua tipuri de thread-uri; id par = thread, id impar = thread virtual  
        if (conversationId % 2 == 0) {
            return new ConversationHandler(conversationId, ui);
        } else {
            return new VirtualConversationHandler(conversationId, ui);
        }
    }
}
