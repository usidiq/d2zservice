package com.d2z.d2zservice.service;

import java.util.List;
import java.util.Map;


import com.d2z.d2zservice.model.CreateConsignmentRequest;
import com.d2z.d2zservice.model.ErrorDetails;
import com.d2z.d2zservice.exception.EtowerFailureResponseException;
import com.d2z.d2zservice.model.SenderDataResponse;

public interface ID2ZAPIService {

	public void createConsignments(CreateConsignmentRequest orderDetail, List<SenderDataResponse> responseList,Map<String, List<ErrorDetails>> errorMap) throws EtowerFailureResponseException;

	public void makeCallToEtowerBasedonSupplierUI(List<String> refNbrs);


}
