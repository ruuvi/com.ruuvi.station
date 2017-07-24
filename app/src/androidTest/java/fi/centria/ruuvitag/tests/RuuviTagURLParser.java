package fi.centria.ruuvitag.tests;

import android.test.AndroidTestCase;

import org.junit.Test;

import fi.centria.ruuvitag.util.Ruuvitag;

import static junit.framework.Assert.assertEquals;

/**
 * Created by admin on 23/07/2017.
 */

public class RuuviTagURLParser
{

        @Test
        public void parserTests() throws Exception
        {
            //

            Ruuvitag tag = new Ruuvitag("0","https://ruu.vi/#BAASADyM5",null,"-87",false);
            assertEquals(tag.getTemperature(),18);

        }

}

