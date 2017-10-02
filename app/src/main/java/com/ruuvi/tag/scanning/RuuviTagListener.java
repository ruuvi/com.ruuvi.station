package com.ruuvi.tag.scanning;

import com.ruuvi.tag.model.RuuviTag;

public interface RuuviTagListener {
    void tagFound(RuuviTag tag);
}
