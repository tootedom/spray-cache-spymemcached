package org.greencheek.elasticacheconfig.decoder;

public enum ConfigInfoDecodingState {
    HEADER,
    VERSION,
    NODES,
    END
}