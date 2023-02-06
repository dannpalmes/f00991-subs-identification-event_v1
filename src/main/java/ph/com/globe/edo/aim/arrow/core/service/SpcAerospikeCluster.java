package ph.com.globe.edo.aim.arrow.core.service;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ph.com.globe.edo.aim.arrow.aerospike.service.cluster.ArrowAerospikeCluster;

@ApplicationScoped
public class SpcAerospikeCluster extends ArrowAerospikeCluster {
	
	@ConfigProperty(name = "aerospike.spc.cluster.name", defaultValue = "spcCluster")
	String spcClusterName;
	
	@ConfigProperty(name = "aerospike.spc.server")
	String spcServer;

	@ConfigProperty(name = "aerospike.spc.port")
	Integer spcPort;
	

	@Override
	public String clusterName() {
		return spcClusterName;
	}

	@Override
	public int port() {
		return spcPort;
	}

	@Override
	public String server() {
		return spcServer;
	}

}
