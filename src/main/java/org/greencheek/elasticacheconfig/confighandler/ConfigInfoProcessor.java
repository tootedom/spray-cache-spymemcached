package org.greencheek.elasticacheconfig.confighandler;

import org.greencheek.elasticacheconfig.domain.ConfigInfo;

/**
 * Created by dominictootell on 20/07/2014.
 */
public interface ConfigInfoProcessor {
    // Will be called on a single thread.
    public void processConfig(ConfigInfo info);
}
