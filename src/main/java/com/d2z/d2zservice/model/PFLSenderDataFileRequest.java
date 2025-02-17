package com.d2z.d2zservice.model;

import java.util.List;

public class PFLSenderDataFileRequest {
	
	private List<SenderData> pflSenderDataApi;
	private List<SenderData> nonPflSenderDataApi;
	
	public List<SenderData> getPflSenderDataApi() {
		return pflSenderDataApi;
	}
	public void setPflSenderDataApi(List<SenderData> pflSenderDataApi) {
		this.pflSenderDataApi = pflSenderDataApi;
	}
	public List<SenderData> getNonPflSenderDataApi() {
		return nonPflSenderDataApi;
	}
	public void setNonPflSenderDataApi(List<SenderData> nonPflSenderDataApi) {
		this.nonPflSenderDataApi = nonPflSenderDataApi;
	}
	
}
