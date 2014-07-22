package org.greencheek.elasticacheconfig.confighandler;

import org.greencheek.elasticacheconfig.domain.ConfigInfo;

/**
 * Created by dominictootell on 20/07/2014.
 */
public interface ConfigInfoProcessor {
    public void processConfig(ConfigInfo info);
}
