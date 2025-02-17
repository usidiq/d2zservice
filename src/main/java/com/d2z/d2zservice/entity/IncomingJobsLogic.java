package com.d2z.d2zservice.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="IncomingJobsLogic")
public class IncomingJobsLogic implements Serializable{
	@Id
	@Column(name = "RowID")
	private String ID;
	
	@Column(name = "MLID")
	private String MLID;
	
	@Column(name = "Broker")
	private String Broker;
	
	@Column(name = "Consignee")
	private String Consignee;

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getMLID() {
		return MLID;
	}

	public void setMLID(String mLID) {
		MLID = mLID;
	}

	public String getBroker() {
		return Broker;
	}

	public void setBroker(String broker) {
		Broker = broker;
	}

	public String getConsignee() {
		return Consignee;
	}

	public void setConsignee(String consignee) {
		Consignee = consignee;
	}

}
