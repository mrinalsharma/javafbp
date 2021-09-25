package com.jpaulmorrison.fbp.core.components.mqtt;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * MQTT In
 * <p>
 *
 *
 */
public class MqttOptions {

	@SerializedName("Server")
	@Expose
	private String server = "localhost";
	@SerializedName("Port")
	@Expose
	private int port = 1883;
	@SerializedName("Protocol")
	@Expose
	private String protocol = "MQTTV3.1.1";
	@SerializedName("ClientId")
	@Expose
	private String clientId = "localhost";
	@SerializedName("KeepAlive")
	@Expose
	private int keepAlive = 60;
	@SerializedName("UserName")
	@Expose
	private String userName = "localhost";
	@SerializedName("Password")
	@Expose
	private String password = "localhost";
	@SerializedName("TLS")
	@Expose
	private Boolean tls;
	@SerializedName("Certificate")
	@Expose
	private String certificate = "";
	@SerializedName("PrivateKey")
	@Expose
	private String privateKey = "";
	@SerializedName("CACertificate")
	@Expose
	private String caCertificate = "";
	@SerializedName("Topic")
	@Expose
	private String topic = "";
	@SerializedName("QOS")
	@Expose
	private int qos = 2;
	@SerializedName("Use Clean Session")
	@Expose
	private Boolean useCleanSession = false;
	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public int getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getTls() {
		return tls;
	}

	public void setTls(Boolean tls) {
		this.tls = tls;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getCaCertificate() {
		return caCertificate;
	}

	public void setCaCertificate(String caCertificate) {
		this.caCertificate = caCertificate;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public Boolean getUseCleanSession() {
		return useCleanSession;
	}

	public void setUseCleanSession(Boolean useCleanSession) {
		this.useCleanSession = useCleanSession;
	}

}