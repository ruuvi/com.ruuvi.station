package com.ruuvi.station.model;

import java.util.List;

/**
 * Created by tmakinen on 29.6.2017.
 */

public class RuuviTagComplexList {
    public List<RuuviTagEntity> ruuviTags;

    public List<RuuviTagEntity> getRuuviTags() {
        return ruuviTags;
    }

    public void setRuuviTags(List<RuuviTagEntity> ruuviTags) {
        this.ruuviTags = ruuviTags;
    }
}
