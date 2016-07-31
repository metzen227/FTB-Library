package com.feed_the_beast.ftbl.api.info;

import com.latmod.lib.io.LMConnection;
import com.latmod.lib.io.RequestMethod;

/**
 * Created by LatvianModder on 09.05.2016.
 */
public enum URLResourceProvider implements IResourceProvider
{
    INSTANCE;

    @Override
    public LMConnection getConnection(String s)
    {
        return new LMConnection(RequestMethod.SIMPLE_GET, s);
    }
}