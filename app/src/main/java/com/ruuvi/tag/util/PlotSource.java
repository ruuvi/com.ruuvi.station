package com.ruuvi.tag.util;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import java.util.Date;
import java.util.Iterator;

import com.ruuvi.tag.model.RuuviTag;
import com.ruuvi.tag.model.ScanEvent;

/**
 * Created by ISOHAJA on 15.7.2017.
 */

public class PlotSource
{
        private static int BUFFER_SIZE = 1000;
        private static PlotSource instance = null;
    private static CircularFifoBuffer buffer;

    protected PlotSource()
        {
            // Exists only to defeat instantiation.
        }
        public static PlotSource getInstance()
        {
            if(instance == null)
            {
                instance = new PlotSource();
                buffer = new CircularFifoBuffer(BUFFER_SIZE);
            }

            return instance;
        }

        public void addScanEvent(ScanEvent event)
        {
            buffer.add(event);
        }

    public Date[] getDomains()
    {
        Date dates[] = new Date[BUFFER_SIZE];

        Iterator itr = buffer.iterator();
        int i = 0;
        while(itr.hasNext())
        {
            ScanEvent s = (ScanEvent) itr.next();
            dates[i] = s.getDate();
            i++;
        }

        return dates;
    }

    public RuuviTag[] getSeriesForTag(String o)
    {
        RuuviTag data[] = new RuuviTag[BUFFER_SIZE];

        Iterator itr = buffer.iterator();
        int i = 0;
        while(itr.hasNext())
        {
            ScanEvent s = (ScanEvent) itr.next();
            data[i] = s.getData(o);
            i++;
        }

        return data;
    }
}
