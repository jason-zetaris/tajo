/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tajo.client;

import com.google.protobuf.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tajo.QueryId;
import org.apache.tajo.annotation.Nullable;
import org.apache.tajo.annotation.ThreadSafe;
import org.apache.tajo.catalog.Schema;
import org.apache.tajo.catalog.TableDesc;
import org.apache.tajo.catalog.TableMeta;
import org.apache.tajo.catalog.partition.PartitionMethodDesc;
import org.apache.tajo.catalog.proto.CatalogProtos;
import org.apache.tajo.ipc.ClientProtos.*;
import org.apache.tajo.jdbc.TajoMemoryResultSet;
import org.apache.tajo.service.ServiceTracker;
import org.apache.tajo.util.KeyValueSet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@ThreadSafe
public class TajoClientImpl extends SessionConnection implements TajoClient, QueryClient, CatalogAdminClient {

  private final Log LOG = LogFactory.getLog(TajoClientImpl.class);
  QueryClient queryClient;
  CatalogAdminClient catalogClient;

  /**
   * Connect to TajoMaster
   *
   * @param tracker ServiceTracker to discovery Tajo Client RPC
   * @param baseDatabase The base database name. It is case sensitive. If it is null,
   *                     the 'default' database will be used.
   * @param properties configurations
   * @throws java.io.IOException
   */
  public TajoClientImpl(ServiceTracker tracker, @Nullable String baseDatabase, KeyValueSet properties)
      throws IOException {
    super(tracker, baseDatabase, properties);

    this.queryClient = new QueryClientImpl(this);
    this.catalogClient = new CatalogAdminClientImpl(this);
  }

  /**
   * Connect to TajoMaster
   *
   * @param addr TajoMaster address
   * @param baseDatabase The base database name. It is case sensitive. If it is null,
   *                     the 'default' database will be used.
   * @param properties configurations
   * @throws java.io.IOException
   */
  public TajoClientImpl(InetSocketAddress addr, @Nullable String baseDatabase, KeyValueSet properties)
      throws IOException {
    this(new DummyServiceTracker(addr), baseDatabase, properties);
  }

  public TajoClientImpl(ServiceTracker serviceTracker) throws IOException {
    this(serviceTracker, null);
  }

  public TajoClientImpl(ServiceTracker serviceTracker, @Nullable String baseDatabase) throws IOException {
    this(serviceTracker, baseDatabase, new KeyValueSet());
  }
  /*------------------------------------------------------------------------*/
  // QueryClient wrappers
  /*------------------------------------------------------------------------*/

  public void closeQuery(final QueryId queryId) {
    queryClient.closeQuery(queryId);
  }

  public void closeNonForwardQuery(final QueryId queryId) {
    queryClient.closeNonForwardQuery(queryId);
  }

  public SubmitQueryResponse executeQuery(final String sql) throws ServiceException {
    return queryClient.executeQuery(sql);
  }

  public SubmitQueryResponse executeQueryWithJson(final String json) throws ServiceException {
    return queryClient.executeQueryWithJson(json);
  }

  public ResultSet executeQueryAndGetResult(final String sql) throws ServiceException, IOException {
    return queryClient.executeQueryAndGetResult(sql);
  }

  public ResultSet executeJsonQueryAndGetResult(final String json) throws ServiceException, IOException {
    return queryClient.executeJsonQueryAndGetResult(json);
  }

  public QueryStatus getQueryStatus(QueryId queryId) throws ServiceException {
    return queryClient.getQueryStatus(queryId);
  }

  public ResultSet getQueryResult(QueryId queryId) throws ServiceException, IOException {
    return queryClient.getQueryResult(queryId);
  }

  public ResultSet createNullResultSet(QueryId queryId) throws IOException {
    return TajoClientUtil.createNullResultSet(queryId);
  }

  public GetQueryResultResponse getResultResponse(QueryId queryId) throws ServiceException {
    return queryClient.getResultResponse(queryId);
  }

  public TajoMemoryResultSet fetchNextQueryResult(final QueryId queryId, final int fetchRowNum) throws ServiceException {
    return queryClient.fetchNextQueryResult(queryId, fetchRowNum);
  }

  public boolean updateQuery(final String sql) throws ServiceException {
    return queryClient.updateQuery(sql);
  }

  public boolean updateQueryWithJson(final String json) throws ServiceException {
    return queryClient.updateQueryWithJson(json);
  }

  public QueryStatus killQuery(final QueryId queryId) throws ServiceException, IOException {
    return queryClient.killQuery(queryId);
  }

  public List<BriefQueryInfo> getRunningQueryList() throws ServiceException {
    return queryClient.getRunningQueryList();
  }

  public List<BriefQueryInfo> getFinishedQueryList() throws ServiceException {
    return queryClient.getFinishedQueryList();
  }

  public List<WorkerResourceInfo> getClusterInfo() throws ServiceException {
    return queryClient.getClusterInfo();
  }

  public QueryInfoProto getQueryInfo(final QueryId queryId) throws ServiceException {
    return queryClient.getQueryInfo(queryId);
  }

  public QueryHistoryProto getQueryHistory(final QueryId queryId) throws ServiceException {
    return queryClient.getQueryHistory(queryId);
  }

  public void setMaxRows(int maxRows) {
	  queryClient.setMaxRows(maxRows);
  }

  public int getMaxRows() {
	  return queryClient.getMaxRows();
  }
  
  /*------------------------------------------------------------------------*/
  // CatalogClient wrappers
  /*------------------------------------------------------------------------*/

  public boolean createDatabase(final String databaseName) throws ServiceException {
    return catalogClient.createDatabase(databaseName);
  }

  public boolean existDatabase(final String databaseName) throws ServiceException {
    return catalogClient.existDatabase(databaseName);
  }

  public boolean dropDatabase(final String databaseName) throws ServiceException {
    return catalogClient.dropDatabase(databaseName);
  }

  public List<String> getAllDatabaseNames() throws ServiceException {
    return catalogClient.getAllDatabaseNames();
  }

  public boolean existTable(final String tableName) throws ServiceException {
    return catalogClient.existTable(tableName);
  }

  public TableDesc createExternalTable(final String tableName, final Schema schema, final URI path,
                                       final TableMeta meta) throws SQLException, ServiceException {
    return catalogClient.createExternalTable(tableName, schema, path, meta);
  }

  public TableDesc createExternalTable(final String tableName, final Schema schema, final URI path,
                                       final TableMeta meta, final PartitionMethodDesc partitionMethodDesc)
      throws SQLException, ServiceException {
    return catalogClient.createExternalTable(tableName, schema, path, meta, partitionMethodDesc);
  }

  public boolean dropTable(final String tableName) throws ServiceException {
    return dropTable(tableName, false);
  }

  public boolean dropTable(final String tableName, final boolean purge) throws ServiceException {
    return catalogClient.dropTable(tableName, purge);
  }

  public List<String> getTableList(@Nullable final String databaseName) throws ServiceException {
    return catalogClient.getTableList(databaseName);
  }

  public TableDesc getTableDesc(final String tableName) throws ServiceException {
    return catalogClient.getTableDesc(tableName);
  }

  public List<CatalogProtos.FunctionDescProto> getFunctions(final String functionName) throws ServiceException {
    return catalogClient.getFunctions(functionName);
  }
}
