package org.dao.clients;

import java.net.InetSocketAddress;

import com.datastax.oss.driver.api.core.CqlSession;

public class CassandraClient {

    private static String keySpace = "job_ks";
    private static String dataCenter = "datacenter1";
    private static String localHost = "localhost";
    private static int defaultCassandraPort = 9042;

    private CqlSession session;

    public CqlSession getSession() {
        if (session == null) {
            try {
                session = CqlSession.builder()
                    .withKeyspace(keySpace)
                    .addContactPoint(new InetSocketAddress(localHost, defaultCassandraPort))
                    .withLocalDatacenter(dataCenter)
                    .build();
            } catch (Exception e) {
                System.err.println("⚠️ Failed to connect to Cassandra: " + e.getMessage()); // change to custom exception and throw error
            }
        }
        return session;
    }
}
