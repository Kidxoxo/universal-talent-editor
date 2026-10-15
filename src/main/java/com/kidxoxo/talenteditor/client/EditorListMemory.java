package com.kidxoxo.talenteditor.client;

import java.util.HashMap;
import java.util.Map;

final class EditorListMemory {
    record View(String query, DefinitionListFilter.State state, String source, int scroll, int selected) {}
    private static final Map<String,View> VIEWS=new HashMap<>();
    static View get(String key){return VIEWS.getOrDefault(key,new View("",DefinitionListFilter.State.ALL,"",0,-1));}
    static void put(String key,View view){VIEWS.put(key,view);}
    static void clear(){VIEWS.clear();}
}
