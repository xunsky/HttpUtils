package xunsky.net.okhttp;

import java.util.HashMap;
class Params {
    HashMap<String,String> params;
    public static Params newInstance(){
        Params params = new Params(new HashMap<String, String>());
        return params;
    }
    public static Params newInstance(HashMap<String,String> map){
        if (map==null)
            throw new RuntimeException("params must not be null = =!");
        Params params = new Params(map);
        return params;
    }
    private Params(HashMap<String,String> map){
        params=map;
    }

    public Params add(String key,String value){
        params.put(key,value);
        return this;
    }
    public HashMap<String,String> commit(){
        return params!=null?params:new HashMap<String,String>();
    }
}
