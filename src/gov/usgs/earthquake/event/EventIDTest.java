package gov.usgs.earthquake.event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EventIDTest {

  private EventID id = null;
  private static String NETWORK = "network";
  private static String CODE = "code";

  @Before
  public void setup () {
    id = new EventID("network", "code");
  }

  @Test
  public void testNetwork () {
    Assert.assertEquals(NETWORK, id.getNetwork());
  }

  @Test
  public void testCode () {
    Assert.assertEquals(CODE, id.getCode());
  }

  @Test
  public void testString () {
    Assert.assertEquals(NETWORK+CODE, id.toString());
  }
}
