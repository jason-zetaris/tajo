package tajo.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import nta.catalog.TableDesc;
import nta.engine.EngineTestingUtils;
import nta.engine.NtaTestingUtility;
import nta.storage.StorageManager;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;

public class TestTajoCluster {
  private static NtaTestingUtility util;
  private static Configuration conf;

  @BeforeClass
  public static void setUp() throws Exception {
    util = new NtaTestingUtility();
    util.startMiniCluster(1);
    conf = util.getConfiguration();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    util.shutdownMiniCluster();
  }

  @Test
  public final void testAttachTable() throws IOException {
    final String tableName = "attach";
    EngineTestingUtils.writeTmpTable(conf, "/tajo/data", tableName, true);
    Configuration conf = util.getConfiguration();
    TajoClient tajo = new TajoClient(conf);
    assertFalse(tajo.existTable(tableName));
    tajo.attachTable(tableName, "/tajo/data/attach");
    assertTrue(tajo.existTable(tableName));
    tajo.detachTable(tableName);
    assertFalse(tajo.existTable(tableName));
  }
  
  @Test
  public final void testUpdateQuery() throws IOException {
    Configuration conf = util.getConfiguration();
    final String tableName = "updateQuery";
    EngineTestingUtils.writeTmpTable(conf, "/tmp", tableName, false);
    StorageManager sm = StorageManager.get(conf, "/tmp");
    FileSystem fs = sm.getFileSystem();
    assertTrue(fs.exists(new Path("/tmp", tableName)));
    
    TajoClient tajo = new TajoClient(conf);
    assertFalse(tajo.existTable(tableName));    
    String tql = 
        "create table " + tableName + " (deptname string, score int) "
        + "using csv location '/tmp/" + tableName + "'";
    tajo.updateQuery(tql);
    assertTrue(tajo.existTable(tableName));
  }

  @Test
  public final void testCreateAndDropTable() throws IOException {
    final String tableName = "create";
    EngineTestingUtils.writeTmpTable(conf, "/tmp", tableName, false);
    StorageManager sm = StorageManager.get(conf, "/tmp");
    FileSystem fs = sm.getFileSystem();
    assertTrue(fs.exists(new Path("/tmp", tableName)));    
    Configuration conf = util.getConfiguration();
    TajoClient tajo = new TajoClient(conf);
    assertFalse(tajo.existTable(tableName));
    tajo.createTable(tableName, new Path("/tmp", tableName), 
        EngineTestingUtils.mockupMeta);
    assertTrue(tajo.existTable(tableName));
    tajo.dropTable(tableName);
    assertFalse(tajo.existTable(tableName));
    assertFalse(fs.exists(new Path("/tmp", tableName)));    
  }
  
  @Test
  public final void testGetClusterInfo() throws IOException, InterruptedException {
    Configuration conf = util.getConfiguration();
    TajoClient tajo = new TajoClient(conf);
    assertEquals(1,tajo.getClusterInfo().size());
  }
  
  @Test
  public final void testGetTableList() throws IOException {    
    final String tableName1 = "table1";
    final String tableName2 = "table2";
    EngineTestingUtils.writeTmpTable(conf, "/tajo/data", tableName1, true);
    EngineTestingUtils.writeTmpTable(conf, "/tajo/data", tableName2, true);
    Configuration conf = util.getConfiguration();
    TajoClient tajo = new TajoClient(conf);
    assertFalse(tajo.existTable(tableName1));
    assertFalse(tajo.existTable(tableName2));
    tajo.attachTable(tableName1, "/tajo/data/"+tableName1);    
    assertTrue(tajo.existTable(tableName1));
    Set<String> tables = Sets.newHashSet(tajo.getTableList());
    assertTrue(tables.contains(tableName1));
    tajo.attachTable(tableName2, "/tajo/data/"+tableName2);
    assertTrue(tajo.existTable(tableName2));
    tables = Sets.newHashSet(tajo.getTableList());
    assertTrue(tables.contains(tableName1));
    assertTrue(tables.contains(tableName2));
  }
  
  @Test
  public final void testGetTableDesc() throws IOException {    
    final String tableName1 = "tabledesc";
    EngineTestingUtils.writeTmpTable(conf, "/tajo/data", tableName1, true);
    Configuration conf = util.getConfiguration();
    TajoClient tajo = new TajoClient(conf);
    assertFalse(tajo.existTable(tableName1));
    tajo.attachTable(tableName1, "/tajo/data/"+tableName1);    
    assertTrue(tajo.existTable(tableName1));
    TableDesc desc = tajo.getTableDesc(tableName1);
    assertNotNull(desc);
    assertEquals(tableName1, desc.getId());
  }
}