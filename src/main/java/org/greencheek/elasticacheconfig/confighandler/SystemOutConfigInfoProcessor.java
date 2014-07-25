package org.greencheek.elasticacheconfig.confighandler;

import org.greencheek.elasticacheconfig.domain.ConfigInfo;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class SystemOutConfigInfoProcessor implements ConfigInfoProcessor {
    @Override
    public void processConfig(ConfigInfo info) {
        System.out.println(info);
    }
}
