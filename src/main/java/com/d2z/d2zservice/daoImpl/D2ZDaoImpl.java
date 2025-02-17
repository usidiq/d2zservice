package com.d2z.d2zservice.daoImpl;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import com.d2z.d2zservice.dao.ID2ZDao;
import com.d2z.d2zservice.entity.APIRates;
import com.d2z.d2zservice.entity.AUPostResponse;
import com.d2z.d2zservice.entity.CSTickets;
import com.d2z.d2zservice.entity.Currency;
import com.d2z.d2zservice.entity.ETowerResponse;
import com.d2z.d2zservice.entity.EbayResponse;
import com.d2z.d2zservice.entity.FastwayPostcode;
import com.d2z.d2zservice.entity.PostcodeZone;
import com.d2z.d2zservice.entity.Returns;
import com.d2z.d2zservice.entity.SenderdataMaster;
import com.d2z.d2zservice.entity.Trackandtrace;
import com.d2z.d2zservice.entity.User;
import com.d2z.d2zservice.entity.UserService;
import com.d2z.d2zservice.exception.ReferenceNumberNotUniqueException;
import com.d2z.d2zservice.model.ClientDashbaord;
import com.d2z.d2zservice.model.CreateEnquiryRequest;
import com.d2z.d2zservice.model.CurrencyDetails;
import com.d2z.d2zservice.model.EditConsignmentRequest;
import com.d2z.d2zservice.model.ResponseMessage;
import com.d2z.d2zservice.model.SenderData;
import com.d2z.d2zservice.model.SenderDataApi;
import com.d2z.d2zservice.model.UserDetails;
import com.d2z.d2zservice.model.auspost.TrackableItems;
import com.d2z.d2zservice.model.auspost.TrackingEvents;
import com.d2z.d2zservice.model.auspost.TrackingResponse;
import com.d2z.d2zservice.model.auspost.TrackingResults;
import com.d2z.d2zservice.model.etower.ETowerTrackingDetails;
import com.d2z.d2zservice.model.etower.EtowerErrorResponse;
import com.d2z.d2zservice.model.etower.GainLabelsResponse;
import com.d2z.d2zservice.model.etower.LabelData;
import com.d2z.d2zservice.model.etower.TrackEventResponseData;
import com.d2z.d2zservice.model.etower.TrackingEventResponse;
import com.d2z.d2zservice.proxy.CurrencyProxy;
import com.d2z.d2zservice.repository.APIRatesRepository;
import com.d2z.d2zservice.repository.AUPostResponseRepository;
import com.d2z.d2zservice.repository.CSTicketsRepository;
import com.d2z.d2zservice.repository.CurrencyRepository;
import com.d2z.d2zservice.repository.ETowerResponseRepository;
import com.d2z.d2zservice.repository.EbayResponseRepository;
import com.d2z.d2zservice.repository.FastwayPostcodeRepository;
import com.d2z.d2zservice.repository.PostcodeZoneRepository;
import com.d2z.d2zservice.repository.ReturnsRepository;
import com.d2z.d2zservice.repository.SenderDataRepository;
import com.d2z.d2zservice.repository.TrackAndTraceRepository;
import com.d2z.d2zservice.repository.UserRepository;
import com.d2z.d2zservice.repository.UserServiceRepository;
import com.d2z.d2zservice.util.D2ZCommonUtil;
import com.d2z.d2zservice.validation.D2ZValidator;
import com.d2z.d2zservice.wrapper.FreipostWrapper;
import com.d2z.singleton.D2ZSingleton;
import com.ebay.soap.eBLBaseComponents.CompleteSaleResponseType;

@Repository
public class D2ZDaoImpl implements ID2ZDao{
	
	@Autowired
	SenderDataRepository senderDataRepository;
	
	@Autowired
	CSTicketsRepository csticketsRepository;

	@Autowired
	TrackAndTraceRepository trackAndTraceRepository;
	
	@Autowired
	PostcodeZoneRepository postcodeZoneRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	UserServiceRepository userServiceRepository;
	
	@Autowired
	EbayResponseRepository ebayResponseRepository;
	
	@Autowired
	ETowerResponseRepository eTowerResponseRepository;
	
	@Autowired
	APIRatesRepository apiRatesRepository;
	
	@Autowired
	FreipostWrapper freipostWrapper;
	
	@Autowired
	AUPostResponseRepository aupostresponseRepository;
	
	@Autowired
	FastwayPostcodeRepository fastwayPostcodeRepository;
	
	@Autowired
	CurrencyRepository currencyRepository;
	
	@Autowired
	CurrencyProxy currencyproxy;
	
	@Autowired
	ReturnsRepository returnsRepository;
	
	@Autowired
	@Lazy
	private D2ZValidator d2zValidator;

	@Override
	public String exportParcel(List<SenderData> orderDetailList,Map<String, LabelData> barcodeMap) {
		Map<String,String> postCodeStateMap = D2ZSingleton.getInstance().getPostCodeStateMap();
		List<String> incomingRefNbr = new ArrayList<String>();
		LabelData provider = null;
		User userInfo = userRepository.findByUsername(orderDetailList.get(0).getUserName());
		List<SenderdataMaster> senderDataList = new ArrayList<SenderdataMaster>();
		String fileSeqId = "D2ZUI"+senderDataRepository.fetchNextSeq().toString();
		for(SenderData senderDataValue: orderDetailList) {
			incomingRefNbr.add(senderDataValue.getReferenceNumber());
			SenderdataMaster senderDataObj = new SenderdataMaster();
			senderDataObj.setSender_Files_ID(fileSeqId);
			senderDataObj.setReference_number(senderDataValue.getReferenceNumber());
			senderDataObj.setConsigneeCompany(senderDataValue.getConsigneeCompany());
			senderDataObj.setConsignee_name(senderDataValue.getConsigneeName());
			senderDataObj.setConsignee_addr1(senderDataValue.getConsigneeAddr1());
			senderDataObj.setConsignee_addr2(senderDataValue.getConsigneeAddr2());
			senderDataObj.setConsignee_Suburb(senderDataValue.getConsigneeSuburb().trim());
			senderDataObj.setConsignee_State(postCodeStateMap.get(senderDataValue.getConsigneePostcode().trim()));
			senderDataObj.setConsignee_Postcode(senderDataValue.getConsigneePostcode().trim());
			senderDataObj.setConsignee_Phone(senderDataValue.getConsigneePhone());
			senderDataObj.setProduct_Description(senderDataValue.getProductDescription());
			senderDataObj.setValue(senderDataValue.getValue());
			senderDataObj.setCurrency(senderDataValue.getCurrency());
			senderDataObj.setShippedQuantity(senderDataValue.getShippedQuantity());
			senderDataObj.setWeight(Double.parseDouble(senderDataValue.getWeight()));
			senderDataObj.setDimensions_Length(senderDataValue.getDimensionsLength());
			senderDataObj.setDimensions_Width(senderDataValue.getDimensionsWidth());
			senderDataObj.setDimensions_Height(senderDataValue.getDimensionsHeight());
			senderDataObj.setServicetype(senderDataValue.getServiceType());
			senderDataObj.setDeliverytype(senderDataValue.getDeliverytype());
			String shipperName = (senderDataValue.getShipperName() != null && !senderDataValue.getShipperName().isEmpty()) ? senderDataValue.getShipperName() : userInfo.getCompanyName();
			senderDataObj.setShipper_Name(shipperName);
			String shipperAddress = (senderDataValue.getShipperAddr1() != null && !senderDataValue.getShipperAddr1().isEmpty()) ? senderDataValue.getShipperAddr1() : userInfo.getAddress();
			senderDataObj.setShipper_Addr1(shipperAddress);
			String shipperCity = (senderDataValue.getShipperCity() != null && !senderDataValue.getShipperCity().isEmpty()) ? senderDataValue.getShipperCity() : userInfo.getSuburb();
			senderDataObj.setShipper_City(shipperCity);
			String shipperState = (senderDataValue.getShipperState() != null && !senderDataValue.getShipperState().isEmpty()) ? senderDataValue.getShipperState() : userInfo.getState();
			senderDataObj.setShipper_State(shipperState);
			String shipperPostcode = (senderDataValue.getShipperPostcode() != null && !senderDataValue.getShipperPostcode().isEmpty()) ? senderDataValue.getShipperPostcode() : userInfo.getPostcode();
			senderDataObj.setShipper_Postcode(shipperPostcode);
			String shipperCountry = (senderDataValue.getShipperCountry() != null && !senderDataValue.getShipperCountry().isEmpty()) ? senderDataValue.getShipperCountry() : userInfo.getCountry();
			senderDataObj.setShipper_Country(shipperCountry);
			senderDataObj.setFilename(senderDataValue.getFileName());
			senderDataObj.setInnerItem(1);
			senderDataObj.setInjectionType(senderDataValue.getInjectionType());
			senderDataObj.setBagId(senderDataValue.getBagId());
			senderDataObj.setUser_ID(senderDataValue.getUserID());
			senderDataObj.setSku(senderDataValue.getSku());
			senderDataObj.setLabelSenderName(senderDataValue.getLabelSenderName());
			senderDataObj.setDeliveryInstructions(senderDataValue.getDeliveryInstructions());
			senderDataObj.setCarrier(senderDataValue.getCarrier());
			senderDataObj.setConsignee_addr2(senderDataValue.getConsigneeAddr2());
			senderDataObj.setConsignee_Email(senderDataValue.getConsigneeEmail());
			senderDataObj.setIsDeleted("N");
			senderDataObj.setTimestamp(D2ZCommonUtil.getAETCurrentTimestamp());
			senderDataObj.setStatus("CONSIGNMENT CREATED");
			senderDataObj.setInjectionType("Direct Injection");
			if("1PM3E".equalsIgnoreCase(senderDataValue.getServiceType()) || "1PME".equalsIgnoreCase(senderDataValue.getServiceType())){
				senderDataObj.setCarrier("Express");
			}else{
				senderDataObj.setCarrier("eParcel");
			}
			if(barcodeMap != null && !barcodeMap.isEmpty())
				provider = barcodeMap.get(barcodeMap.keySet().toArray()[0]);
			
			if(null!= barcodeMap && !barcodeMap.isEmpty() && provider.getProvider().equalsIgnoreCase("Etower") 
					&& barcodeMap.containsKey(senderDataValue.getReferenceNumber())) {
				LabelData labelData= barcodeMap.get(senderDataValue.getReferenceNumber());
				senderDataObj.setBarcodelabelNumber(labelData.getBarCode());
				senderDataObj.setArticleId(labelData.getArticleId());		        
				senderDataObj.setDatamatrix(D2ZCommonUtil.formatDataMatrix(labelData.getBarCode2D().replaceAll("\\(|\\)|\u001d", "")));
				senderDataObj.setInjectionState(senderDataValue.getInjectionState());
			}else if(null!= barcodeMap && !barcodeMap.isEmpty() && provider.getProvider().equalsIgnoreCase("PFL") && 
					barcodeMap.containsKey(senderDataValue.getReferenceNumber())) {
				LabelData pflLabel= barcodeMap.get(senderDataValue.getReferenceNumber());
				senderDataObj.setInjectionState(pflLabel.getHub());
				senderDataObj.setBarcodelabelNumber(pflLabel.getTrackingNo());
				senderDataObj.setArticleId(pflLabel.getTrackingNo());
				senderDataObj.setMlid(pflLabel.getArticleId());
				senderDataObj.setDatamatrix(pflLabel.getMatrix());
				senderDataObj.setCarrier("FastwayM");
			}else if(null!= barcodeMap && !barcodeMap.isEmpty() && provider.getProvider().equalsIgnoreCase("PCA") && 
					barcodeMap.containsKey(senderDataValue.getReferenceNumber())) {
				LabelData pflLabel= barcodeMap.get(senderDataValue.getReferenceNumber());
				senderDataObj.setInjectionState(pflLabel.getHub());
				senderDataObj.setBarcodelabelNumber(pflLabel.getTrackingNo());
				senderDataObj.setArticleId(pflLabel.getTrackingNo());
				senderDataObj.setMlid(pflLabel.getArticleId());
				senderDataObj.setDatamatrix(pflLabel.getMatrix());
				senderDataObj.setCarrier("FastwayS");
			}
			senderDataList.add(senderDataObj);
		}
		senderDataRepository.saveAll(senderDataList);
		System.out.println("create consignment UI object construction Done data got inserted--->"+senderDataList.size());
		storProcCall(fileSeqId);
		updateTrackAndTrace(fileSeqId,userInfo.getUser_Id(),null);
		return fileSeqId;
	}

	@Override
	public List<String> fileList(Integer userId) {
		List<String> listOfFileNames= senderDataRepository.fetchFileName(userId);
		return listOfFileNames;
	}
	
	@Override
	public List<String> labelFileList(Integer userId) {
		List<String> listOfFileNames= senderDataRepository.fetchLabelFileName(userId);
		return listOfFileNames;
	}

	@Override
	public List<SenderdataMaster> consignmentFileData(String fileName) {
		List<SenderdataMaster> listOfFileNames= senderDataRepository.fetchConsignmentData(fileName);
		return listOfFileNames;
	}
	
	@Override
	public List<SenderdataMaster> fetchManifestData(String fileName) {
		List<SenderdataMaster> allConsignmentData= senderDataRepository.fetchManifestData(fileName);
		return allConsignmentData;
	}

	@Override
	public String consignmentDelete(String refrenceNumlist) {
		//Calling Delete Store Procedure
		senderDataRepository.consigneeDelete(refrenceNumlist);
		return "Selected Consignments Deleted Successfully";
	}

	@Override
	public List<String> trackingDetails(String fileName) {
		List<String> trackingDetails= senderDataRepository.fetchTrackingDetails(fileName);
		return trackingDetails;
	}

	@Override
	public List<String> trackingLabel(List<String> refBarNum) {
		//String trackingDetails= senderDataRepository.fetchTrackingLabel(refBarNum);
		List<String> trackingDetails= senderDataRepository.fetchTrackingLabel(refBarNum);
		System.out.println(trackingDetails.size());
		return trackingDetails;
	}

	@Override
	public String manifestCreation(String manifestNumber, String[] refrenceNumber) {
		//Calling Delete Store Procedure
		senderDataRepository.manifestCreation(manifestNumber, refrenceNumber);
		return "Manifest Updated Successfully";
	}

	public List<Trackandtrace> trackParcel(String refNbr) {
		List<Trackandtrace> trackAndTrace = trackAndTraceRepository.fetchTrackEventByRefNbr(refNbr);
		return trackAndTrace;
	}

	@Override
	public String createConsignments(List<SenderDataApi> orderDetailList, int userId, String userName, Map<String,LabelData> barcodeMap) {
		Map<String,String> postCodeStateMap = D2ZSingleton.getInstance().getPostCodeStateMap();
		List<SenderdataMaster> senderDataList = new ArrayList<SenderdataMaster>();
		LabelData provider = null;
		List<String> autoShipRefNbrs = new ArrayList<String>();
		
		User userInfo = userRepository.findByUsername(userName);
		boolean autoShipment =("Y").equals( userInfo.getAutoShipment());
		String fileSeqId = "D2ZAPI"+senderDataRepository.fetchNextSeq();
		System.out.println("create consignment API object construction --->"+orderDetailList.size());
		for(SenderDataApi senderDataValue: orderDetailList) {
			SenderdataMaster senderDataObj = new SenderdataMaster();
			senderDataObj.setUser_ID(userId);
			senderDataObj.setSender_Files_ID(fileSeqId);
			senderDataObj.setReference_number(senderDataValue.getReferenceNumber());
			senderDataObj.setConsigneeCompany(senderDataValue.getConsigneeCompany());
			senderDataObj.setConsignee_name(senderDataValue.getConsigneeName());
			senderDataObj.setConsignee_addr1(senderDataValue.getConsigneeAddr1());
			senderDataObj.setConsignee_addr2(senderDataValue.getConsigneeAddr2());
			senderDataObj.setConsignee_Suburb(senderDataValue.getConsigneeSuburb());
			senderDataObj.setConsignee_State(postCodeStateMap.get(senderDataValue.getConsigneePostcode()));
			senderDataObj.setConsignee_Postcode(senderDataValue.getConsigneePostcode());
			senderDataObj.setConsignee_Phone(senderDataValue.getConsigneePhone());
			senderDataObj.setProduct_Description(senderDataValue.getProductDescription());
			senderDataObj.setValue(senderDataValue.getValue());
			senderDataObj.setCurrency(senderDataValue.getCurrency());
			senderDataObj.setShippedQuantity(senderDataValue.getShippedQuantity());
			senderDataObj.setWeight(Double.valueOf(senderDataValue.getWeight()));
			senderDataObj.setDimensions_Length(senderDataValue.getDimensionsLength());
			senderDataObj.setDimensions_Width(senderDataValue.getDimensionsWidth());
			senderDataObj.setDimensions_Height(senderDataValue.getDimensionsHeight());
			senderDataObj.setServicetype(senderDataValue.getServiceType());
			senderDataObj.setDeliverytype(senderDataValue.getDeliverytype());
			String shipperName = (senderDataValue.getShipperName() != null && !senderDataValue.getShipperName().isEmpty()) ? senderDataValue.getShipperName() : userInfo.getCompanyName();
			senderDataObj.setShipper_Name(shipperName);
			String shipperAddress = (senderDataValue.getShipperAddr1() != null && !senderDataValue.getShipperAddr1().isEmpty()) ? senderDataValue.getShipperAddr1() : userInfo.getAddress();
			senderDataObj.setShipper_Addr1(shipperAddress);
			String shipperCity = (senderDataValue.getShipperCity() != null && !senderDataValue.getShipperCity().isEmpty()) ? senderDataValue.getShipperCity() : userInfo.getSuburb();
			senderDataObj.setShipper_City(shipperCity);
			String shipperState = (senderDataValue.getShipperState() != null && !senderDataValue.getShipperState().isEmpty()) ? senderDataValue.getShipperState() : userInfo.getState();
			senderDataObj.setShipper_State(shipperState);
			String shipperPostcode = (senderDataValue.getShipperPostcode() != null && !senderDataValue.getShipperPostcode().isEmpty()) ? senderDataValue.getShipperPostcode() : userInfo.getPostcode();
			senderDataObj.setShipper_Postcode(shipperPostcode);
			String shipperCountry = (senderDataValue.getShipperCountry() != null && !senderDataValue.getShipperCountry().isEmpty()) ? senderDataValue.getShipperCountry() : userInfo.getCountry();
			senderDataObj.setShipper_Country(shipperCountry);
			senderDataObj.setFilename("D2ZAPI"+D2ZCommonUtil.getCurrentTimestamp());
			//senderDataObj.setFilename(senderDataValue.getFileName());
			senderDataObj.setSku(senderDataValue.getSku());
			senderDataObj.setLabelSenderName(senderDataValue.getLabelSenderName());
			senderDataObj.setDeliveryInstructions(senderDataValue.getDeliveryInstructions());
			if(senderDataValue.getBarcodeLabelNumber()!=null && !senderDataValue.getBarcodeLabelNumber().trim().isEmpty()
					&& senderDataValue.getDatamatrix()!=null && !senderDataValue.getDatamatrix().trim().isEmpty())
			{
				senderDataObj.setBarcodelabelNumber(senderDataValue.getBarcodeLabelNumber());
				senderDataObj.setArticleId(senderDataValue.getBarcodeLabelNumber().substring(18));
				if(senderDataValue.getBarcodeLabelNumber().length() == 41)
				senderDataObj.setMlid(senderDataValue.getBarcodeLabelNumber().substring(18,23));
				else if(senderDataValue.getBarcodeLabelNumber().length() == 39)
				senderDataObj.setMlid(senderDataValue.getBarcodeLabelNumber().substring(18,21));

			
				senderDataObj.setDatamatrix(senderDataValue.getDatamatrix());
				if(autoShipment)
					autoShipRefNbrs.add(senderDataValue.getReferenceNumber());
			}
			if(senderDataValue.getInjectionState()!=null)
			{
				senderDataObj.setInjectionState(senderDataValue.getInjectionState());
			}
			if("1PM3E".equalsIgnoreCase(senderDataValue.getServiceType())){
				senderDataObj.setCarrier("Express");
			}else if(null == senderDataValue.getCarrier() || senderDataValue.getCarrier().isEmpty()){
				senderDataObj.setCarrier("eParcel");
			}
			else {
				senderDataObj.setCarrier(senderDataValue.getCarrier());
			}
			senderDataObj.setConsignee_Email(senderDataValue.getConsigneeEmail());
			senderDataObj.setStatus("CONSIGNMENT CREATED");
			senderDataObj.setInjectionType("Direct Injection");
			senderDataObj.setTimestamp(D2ZCommonUtil.getAETCurrentTimestamp());
			senderDataObj.setIsDeleted("N");
			if(barcodeMap != null && !barcodeMap.isEmpty())
				provider = barcodeMap.get(barcodeMap.keySet().toArray()[0]);
			if(null!= barcodeMap && !barcodeMap.isEmpty() && provider.getProvider().equalsIgnoreCase("Etower") && 
						barcodeMap.containsKey(senderDataValue.getReferenceNumber())) {
				LabelData labelData= barcodeMap.get(senderDataValue.getReferenceNumber());
				senderDataObj.setBarcodelabelNumber(labelData.getBarCode());
				senderDataObj.setArticleId(labelData.getArticleId());		        
				senderDataObj.setDatamatrix(D2ZCommonUtil.formatDataMatrix(labelData.getBarCode2D().replaceAll("\\(|\\)|\u001d", "")));
				senderDataObj.setInjectionState(senderDataValue.getInjectionState());
			}else if(null!= barcodeMap && !barcodeMap.isEmpty() && provider.getProvider().equalsIgnoreCase("PFL") && 
						barcodeMap.containsKey(senderDataValue.getReferenceNumber())) {
				LabelData pflLabel= barcodeMap.get(senderDataValue.getReferenceNumber());
				senderDataObj.setInjectionState(pflLabel.getHub());
				senderDataObj.setBarcodelabelNumber(pflLabel.getTrackingNo());
				senderDataObj.setArticleId(pflLabel.getArticleId());
				senderDataObj.setDatamatrix(pflLabel.getMatrix());
				senderDataObj.setCarrier("FastwayM");
			}else if(null!= barcodeMap && !barcodeMap.isEmpty() && provider.getProvider().equalsIgnoreCase("PCA") && 
						barcodeMap.containsKey(senderDataValue.getReferenceNumber())) {
				LabelData pflLabel= barcodeMap.get(senderDataValue.getReferenceNumber());
				senderDataObj.setInjectionState(pflLabel.getHub());
				senderDataObj.setBarcodelabelNumber(pflLabel.getTrackingNo());
				senderDataObj.setArticleId(pflLabel.getArticleId());
				senderDataObj.setDatamatrix(pflLabel.getMatrix());
				senderDataObj.setCarrier(pflLabel.getCarrier());
			}
			senderDataList.add(senderDataObj);
		}
		List<SenderdataMaster> insertedOrder = (List<SenderdataMaster>) senderDataRepository.saveAll(senderDataList);
		System.out.println("create consignment API object construction Done data got inserted--->"+insertedOrder.size());
		if(orderDetailList.get(0).getBarcodeLabelNumber()==null || orderDetailList.get(0).getBarcodeLabelNumber().trim().isEmpty()
				|| orderDetailList.get(0).getDatamatrix()==null || orderDetailList.get(0).getDatamatrix().trim().isEmpty())
		{
		storProcCall(fileSeqId);
		}
		updateTrackAndTrace(fileSeqId,userId,autoShipRefNbrs);
		return fileSeqId;
	}
	
	/**
	 * @param senderDataList
	 */

	public void updateTrackAndTrace(String fileSeqId,int userId,List<String> autoShipRefNbrs) {
		Runnable r = new Runnable() {			
	        public void run() {
	        	
	        	List<String> insertedOrder = fetchBySenderFileID(fileSeqId);
	        	List<Trackandtrace> trackAndTraceList = new ArrayList<Trackandtrace>();
	        	Iterator itr = insertedOrder.iterator();
				while (itr.hasNext()) {
					Object[] obj = (Object[]) itr.next();
	    			Trackandtrace trackAndTrace = new Trackandtrace();
	    			//trackAndTrace.setRowId(D2ZCommonUtil.generateTrackID());
	    			trackAndTrace.setUser_Id(String.valueOf(userId));
	    			trackAndTrace.setReference_number(obj[0].toString());
	    			trackAndTrace.setTrackEventCode("CC");
	    			trackAndTrace.setTrackEventDetails("CONSIGNMENT CREATED");
	    			trackAndTrace.setTrackEventDateOccured(D2ZCommonUtil.getAETCurrentTimestamp());
	    			trackAndTrace.setCourierEvents(null);
	    			trackAndTrace.setTrackSequence(1);
	    			trackAndTrace.setBarcodelabelNumber(obj[3].toString());
	    			trackAndTrace.setFileName("SP");
	    			trackAndTrace.setAirwayBill(null);
	    			trackAndTrace.setSignerName(null);
	    			trackAndTrace.setSignature(null);
	    			trackAndTrace.setIsDeleted("N");
	    			trackAndTrace.setTimestamp(D2ZCommonUtil.getAETCurrentTimestamp());
	    			trackAndTrace.setArticleID(obj[2].toString());
	    			trackAndTraceList.add(trackAndTrace);
	    		}
	    		List<Trackandtrace> trackAndTraceInsert = (List<Trackandtrace>) trackAndTraceRepository.saveAll(trackAndTraceList);
	    		if(null!=autoShipRefNbrs && !autoShipRefNbrs.isEmpty()) {
	    			System.out.println("Auto-Shipment Allocation");
	    			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	    			String shipmentNumber = userId+simpleDateFormat.format(new Date());
	    			allocateShipment(String.join(",", autoShipRefNbrs), shipmentNumber);
	    			List<SenderdataMaster> senderMasterData = fetchDataBasedonSupplier(autoShipRefNbrs,"Freipost");
		        	if(!senderMasterData.isEmpty()) {
		        		freipostWrapper.uploadManifestService(senderMasterData);
		        	}
		        	
	    		}
	        }};
	        new Thread(r).start();
	}
	
	public synchronized  void storProcCall(String fileSeqId) {
		System.out.println("Before calling the store procedure, Sequence Id --->"+fileSeqId);
		System.out.println("Before the store procedure call, Timing --->"+java.time.LocalDateTime.now());
		senderDataRepository.inOnlyTest(fileSeqId);
		System.out.println("After the store procedure call, Timing --->"+java.time.LocalDateTime.now());
		System.out.println("After calling the store procedure, Sequence Id --->"+fileSeqId);
	}
	
    public List<PostcodeZone> fetchAllPostCodeZone(){
    	List<PostcodeZone> postCodeZoneList= (List<PostcodeZone>) postcodeZoneRepository.findAll();
    	System.out.println(postCodeZoneList.size());
    	return postCodeZoneList;
    }
    
    public List<String> fetchAllReferenceNumbers(){
    	List<String> referenceNumber_DB= senderDataRepository.fetchAllReferenceNumbers();
    	return referenceNumber_DB;
    }

	@Override
	public List<String> fetchBySenderFileID(String senderFileID) {
		List<String> senderDataMaster = senderDataRepository.fetchBySenderFileId(senderFileID);
		return senderDataMaster;
	}

	@Override
	public List<Trackandtrace> trackParcelByArticleID(String articleID) {
		List<Trackandtrace> trackAndTrace = trackAndTraceRepository.fetchTrackEventByArticleID(articleID);
		return trackAndTrace;
	}

	@Override

public ResponseMessage editConsignments(List<EditConsignmentRequest> requestList) {
		/*requestList.forEach(obj->{
			senderDataRepository.editConsignments(obj.getReferenceNumber(), obj.getWeight());
		});*/
		List<String> incorrectRefNbrs = new ArrayList<String>();
		int updatedRows=0;
		//Timestamp start = Timestamp.from(Instant.now());
		List<SenderdataMaster> senderDataList = new ArrayList<SenderdataMaster>();
		for(EditConsignmentRequest obj : requestList) {
			SenderdataMaster senderData = senderDataRepository.fetchByReferenceNumbers(obj.getReferenceNumber());
			if(senderData!=null) {
			updatedRows++;
			senderData.setWeight(obj.getWeight());
			senderDataList.add(senderData);
		}
			else {
				incorrectRefNbrs.add(obj.getReferenceNumber());
			}
		}
		senderDataRepository.saveAll(senderDataList);
		/*Timestamp end = Timestamp.from(Instant.now());
		long callDuration = end.getTime() - start.getTime();
		System.out.println("Call Duration : "+callDuration);*/
		ResponseMessage responseMsg = new ResponseMessage();
		if(updatedRows==0) {
			responseMsg.setResponseMessage("Update failed");
		}else if(updatedRows == requestList.size()) {
			responseMsg.setResponseMessage("Weight updated successfully");
		}
		else {
			responseMsg.setResponseMessage("Partially updated");
		}
		responseMsg.setMessageDetail(incorrectRefNbrs);
		return responseMsg;
	}

	@Override
	public String allocateShipment(String referenceNumbers, String shipmentNumber) {
		senderDataRepository.updateAirwayBill(referenceNumbers.split(","), shipmentNumber,D2ZCommonUtil.getAETCurrentTimestamp());
		senderDataRepository.allocateShipment(referenceNumbers, shipmentNumber);
		return "Shipment Allocated Successfully";
	}
	

	@Override
	public User addUser(UserDetails userData) {
		User userObj =new User();
		userObj.setCompanyName(userData.getCompanyName());
		userObj.setAddress(userData.getAddress());
		userObj.setSuburb(userData.getSuburb());
		userObj.setState(userData.getState());
		userObj.setPostcode(userData.getPostCode());
		userObj.setCountry(userData.getCountry());
		userObj.setEmail(userData.getEmailAddress());
		userObj.setUsername(userData.getUserName());
		userObj.setPassword(D2ZCommonUtil.hashPassword(userData.getPassword()));
		userObj.setRole_Id(userData.getRole_Id());
		userObj.setName(userData.getContactName());
		userObj.setPhoneNumber(userData.getContactPhoneNumber());
		userObj.setUser_IsDeleted(false);
		userObj.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
		userObj.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
		userObj.setClientBrokerId(userData.getClientBroker());
		userObj.setEBayToken(userData.geteBayToken());
		userObj.setPassword_value(userData.getPassword());
		User savedUser = userRepository.save(userObj);
		return savedUser;
	}

	@Override
	public List<UserService> addUserService(User user,List<String> serviceTypeList) {
		List<UserService> userServiceList = new ArrayList<UserService>();
		for(String serviceType : serviceTypeList) {
		UserService userService = new UserService();
		userService.setUserId(user.getUser_Id());
		userService.setCompanyName(user.getCompanyName());
		userService.setUser_Name(user.getUsername());
		userService.setServiceType(serviceType);
		if(serviceType.equalsIgnoreCase("UnTracked")) {
			userService.setInjectionType("Origin Injection");
		}else {
			userService.setInjectionType("Direct Injection");
		}
		userService.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
		userService.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
		userServiceList.add(userService);
		}
		List<UserService> savedUserService = (List<UserService>) userServiceRepository.saveAll(userServiceList);
		return savedUserService;
	}


	@Override
	public User updateUser(User existingUser) {
		User updateduser = userRepository.save(existingUser);
		return updateduser;
	}

	@Override
	public void updateUserService(User existingUser, UserDetails userDetails) {
		List<UserService> userServiceList = new ArrayList<UserService>();
		if(!userDetails.getServiceType().isEmpty()) {
			for(String serviceType : userDetails.getServiceType() ) {
				UserService userService  = userServiceRepository.fetchbyCompanyNameAndServiceType(existingUser.getCompanyName(), serviceType,userDetails.getUserName());
				if(userService == null) {
					UserService newUserService = new UserService();
					newUserService.setUserId(existingUser.getUser_Id());
					newUserService.setCompanyName(existingUser.getCompanyName());
					newUserService.setUser_Name(existingUser.getUsername());
					newUserService.setServiceType(serviceType);
					newUserService.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
					newUserService.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
					userServiceList.add(newUserService);			
					}
				else {
					if(userService.isService_isDeleted()) {
						userService.setService_isDeleted(false);
						userService.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
						userServiceList.add(userService);
					}
				}
			}
		}
		if(!userDetails.getDeletedServiceTypes().isEmpty()) {
			
			for(String serviceType : userDetails.getDeletedServiceTypes() ) {
				
				UserService userService  = userServiceRepository.fetchbyCompanyNameAndServiceType(existingUser.getCompanyName(), serviceType,userDetails.getUserName());
			
				if(userService!=null) {
					userService.setService_isDeleted(true);
					userService.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
					userServiceList.add(userService);
				}
				
		}
		}
	
		userServiceRepository.saveAll(userServiceList);

	}

	/*private void deleteUserService(User existingUser, List<String> deletedServiceTypes) {
		List<UserService> userServiceList = new ArrayList<UserService>();
		if(!deletedServiceTypes.isEmpty()) {
		for(String serviceType : deletedServiceTypes ) {
			UserService userService  = userServiceRepository.fetchbyCompanyNameAndServiceType(existingUser.getCompanyName(), serviceType);
			if(userService!=null) {
				userService.setService_isDeleted(true);
				userService.setModifiedTimestamp(Timestamp.from(Instant.now()));
				userServiceList.add(userService);
			}
			
	}
		userServiceRepository.saveAll(userServiceList);
	}
	}*/

	@Override
	public String deleteUser(String companyName, String roleId) {
		User existingUser = userRepository.fetchUserbyCompanyName(companyName, Integer.parseInt(roleId));
		if(existingUser==null) {
			return "Company Name does not exist";
		}
		else {
			existingUser.setUser_IsDeleted(true);
			existingUser.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
			userRepository.save(existingUser);
			List<UserService> userService_DB  = userServiceRepository.fetchbyCompanyName(companyName);
			List<UserService> userServiceList = new ArrayList<UserService>();
			for(UserService userService: userService_DB) {
				userService.setService_isDeleted(true);
				userService.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
				userServiceList.add(userService);
			}
			userServiceRepository.saveAll(userServiceList);
		}
		return "User deleted successfully";
	}

	public User login(String userName, String passWord) {
		User userDaetils = userRepository.fetchUserDetails(userName, passWord);
		return userDaetils;
	}

	@Override
	public List<SenderdataMaster> fetchShipmentData(String shipmentNumber, List<Integer> clientIds) {
		List<SenderdataMaster> senderData = senderDataRepository.fetchShipmentData(shipmentNumber, clientIds);
		return senderData;
	}

	@Override
	public List<String> fetchServiceTypeByUserName(String userName) {
		List<String> serviceTypeList = userServiceRepository.fetchAllServiceTypeByUserName(userName);
		return serviceTypeList;
	}

	@Override
	public Trackandtrace getLatestStatusByReferenceNumber(String referenceNumber) {
		List<Trackandtrace> trackAndTraceList =  trackAndTraceRepository.fetchTrackEventByRefNbr(referenceNumber);
		Trackandtrace trackandTrace = null;
		if(!trackAndTraceList.isEmpty()) {
			trackandTrace = trackAndTraceList.get(0);
		}
		return trackandTrace;
	}
	
	@Override
	public List<String> fetchReferenceNumberByUserId(Integer userId) {
		List<String> referenceNumbers_DB = senderDataRepository.fetchReferenceNumberByUserId(userId);
		return referenceNumbers_DB;
	}

	@Override
	public Trackandtrace getLatestStatusByArticleID(String articleID) {
		List<Trackandtrace> trackAndTraceList =  trackAndTraceRepository.fetchTrackEventByArticleID(articleID);
		Trackandtrace trackandTrace = null;
		if(!trackAndTraceList.isEmpty()) {
			trackandTrace = trackAndTraceList.get(0);
		}
		return trackandTrace;
	}

	@Override
	public List<SenderdataMaster> findRefNbrByShipmentNbr(String[] referenceNumbers) {
		return senderDataRepository.findRefNbrByShipmentNbr(referenceNumbers);
	}

	@Override
	public void logEbayResponse(CompleteSaleResponseType response) {
				EbayResponse resp = new EbayResponse();
				resp.setAck(response.getAck().toString());
				if(null!= response.getErrors() && response.getErrors().length>0) {
				resp.setShortMessage(response.getErrors(0).getShortMessage());
				resp.setLongMessage(response.getErrors(0).getLongMessage());
				}
				ebayResponseRepository.save(resp);
	}
	
	public ClientDashbaord clientDahbaord(Integer userId) {
		ClientDashbaord clientDashbaord = new ClientDashbaord();
		clientDashbaord.setConsignmentsCreated(senderDataRepository.fecthConsignmentsCreated(userId));
		clientDashbaord.setConsignmentsManifested(senderDataRepository.fetchConsignmentsManifested(userId));
		clientDashbaord.setConsignmentsManifests(senderDataRepository.fetchConsignmentsManifests(userId));
		clientDashbaord.setConsignmentsDeleted(senderDataRepository.fetchConsignmentsDeleted(userId));
		clientDashbaord.setConsignmentDelivered(senderDataRepository.fetchConsignmentDelivered(userId));
		return clientDashbaord;
	}

	@Override
	public void deleteConsignment(String referenceNumbers) {
		senderDataRepository.deleteConsignments(referenceNumbers);
	}

	@Override
	public List<String> fetchServiceType(Integer user_id) {
		List<String> userServiceType = userServiceRepository.fetchUserServiceById(user_id);
		return userServiceType;
	}

	@Override
	public List<APIRates> fetchAllAPIRates() {
		List<APIRates> apiRates= (List<APIRates>) apiRatesRepository.findAll();
    	System.out.println(apiRates.size());
    	return apiRates;
	}

	@Override
	public void logEtowerResponse(List<ETowerResponse> responseEntity) {
		eTowerResponseRepository.saveAll(responseEntity);
		
	}
	@Override
	public ResponseMessage insertTrackingDetails(TrackingEventResponse trackEventresponse) {
		List<Trackandtrace> trackAndTraceList = new ArrayList<Trackandtrace>();
		List<TrackEventResponseData> responseData = trackEventresponse.getData();
		ResponseMessage responseMsg =  new ResponseMessage();

		if(responseData!=null && responseData.isEmpty()) {
			responseMsg.setResponseMessage("No Data from ETower");
		}
		else {
		
		for(TrackEventResponseData data : responseData ) {
		if(data!=null &&  data.getEvents()!=null) {
			
			for(ETowerTrackingDetails trackingDetails : data.getEvents()) {
				
				Trackandtrace trackandTrace = new Trackandtrace();
				trackandTrace.setArticleID(trackingDetails.getTrackingNo());
				trackandTrace.setFileName("eTowerAPI");
		
                trackandTrace.setTrackEventDateOccured(trackingDetails.getEventTime());
				trackandTrace.setTrackEventCode(trackingDetails.getEventCode());
			
				trackandTrace.setTrackEventDetails(trackingDetails.getActivity());
				trackandTrace.setCourierEvents(trackingDetails.getActivity());
				trackandTrace.setTimestamp(Timestamp.valueOf(LocalDateTime.now()).toString());
				trackandTrace.setLocation(trackingDetails.getLocation());
				trackandTrace.setIsDeleted("N");
				if("ARRIVED AT DESTINATION AIRPORT".equalsIgnoreCase(trackandTrace.getTrackEventDetails()) ||
						("COLLECTED FROM AIRPORT TERMINAL".equalsIgnoreCase(trackandTrace.getTrackEventDetails())) ||
							("PREPARING TO DISPATCH".equalsIgnoreCase(trackandTrace.getTrackEventDetails())))
					{
					trackandTrace.setIsDeleted("Y");
					}
				trackAndTraceList.add(trackandTrace);
			}
			
		}
		}
		trackAndTraceRepository.saveAll(trackAndTraceList);
		trackAndTraceRepository.updateTracking();
		trackAndTraceRepository.deleteDuplicates();
		responseMsg.setResponseMessage("Data uploaded successfully from ETower");
		}
		return responseMsg;
	}
	public List<SenderdataMaster> fetchConsignmentsManifestShippment(List<String> incomingRefNbr){
		return senderDataRepository.fetchConsignmentsManifestShippment(incomingRefNbr);
	}

	@Override
	public List<SenderdataMaster> fetchDataForAusPost(List<String> refNbrs) {
		// TODO Auto-generated method stub
		return senderDataRepository.fetchDataForAusPost(refNbrs);
	}


	@Override
	public int fetchUserIdByReferenceNumber(String reference_number) {
		int userID = senderDataRepository.fetchUserIdByReferenceNumber(reference_number);
		return userID;
	}


	@Override
	public List<String>  fetchArticleIDForFDMCall() {
		List<String> referenceNumber = trackAndTraceRepository.fetchArticleIDForFDMCall();
		//String[] refArray =referenceNumber.stream().toArray(String[]::new);
		return referenceNumber;
	}

	private Map<String, LabelData> processGainLabelsResponse(GainLabelsResponse response) {
		Map<String, LabelData> barcodeMap= new HashMap<String,LabelData>();
		List<ETowerResponse> responseEntity = new ArrayList<ETowerResponse>();
		 if(response!=null) {
   			List<LabelData> responseData = response.getData();
   			if(responseData== null && null!=response.getErrors()) {
   				 for(EtowerErrorResponse error : response.getErrors()) {
	     				ETowerResponse errorResponse  = new ETowerResponse();
   				 	errorResponse.setAPIName("Gain Labels");
	     			 	errorResponse.setStatus(response.getStatus());
   				 	errorResponse.setErrorCode(error.getCode());
   				 	errorResponse.setErrorMessage(error.getMessage());
   				 	responseEntity.add(errorResponse);
   				}
   			}
   			
   			for(LabelData data : responseData) {
   				List<EtowerErrorResponse> errors = data.getErrors();
   				if(null == errors) {
   				ETowerResponse errorResponse  = new ETowerResponse();
				 	errorResponse.setAPIName("Gain Labels");
   			 	errorResponse.setStatus(data.getStatus());
   			 	errorResponse.setOrderId(data.getOrderId());
   		 		errorResponse.setReferenceNumber(data.getReferenceNo());
   		 		errorResponse.setTrackingNo(data.getTrackingNo());
   		 		errorResponse.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
   		 		responseEntity.add(errorResponse);
   		 		barcodeMap.put(data.getReferenceNo(), data);
   				}
   				else {
   				 for(EtowerErrorResponse error : errors) {
   					ETowerResponse errorResponse  = new ETowerResponse();
	     			 	errorResponse.setAPIName("Gain Labels");
	     			 	errorResponse.setStatus(response.getStatus());
	     			 	errorResponse.setStatus(data.getStatus());
	     			 	errorResponse.setOrderId(data.getOrderId());
	     		 		errorResponse.setReferenceNumber(data.getReferenceNo());
	     		 		errorResponse.setTrackingNo(data.getTrackingNo());
	     		 		errorResponse.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
   				    errorResponse.setErrorCode(error.getCode());
   				 	errorResponse.setErrorMessage(error.getMessage());
  				 	responseEntity.add(errorResponse);
   				}
   				}
   			}
   			}
   				logEtowerResponse(responseEntity);
   				
		return barcodeMap;
		
	}

	@Override
	public List<String>  fetchDataForAUPost() {
		List<Trackandtrace> trackandtraceData = trackAndTraceRepository.fetchArticleIDForAUPost();
		List<Trackandtrace> updatedData = new ArrayList<Trackandtrace>();
		List<String> refNbrs = new ArrayList<String>(); 
		if(trackandtraceData.size() >= 10) {
			for(Trackandtrace data : trackandtraceData) {
				data.setFileName("AUPost");
				updatedData.add(data);
				refNbrs.add(data.getReference_number());
			}
			trackAndTraceRepository.saveAll(updatedData);
		}
		
		return refNbrs;
	}
	public ResponseMessage insertAUTrackingDetails(TrackingResponse auTrackingDetails,Map<String,String> map) {
		List<Trackandtrace> trackAndTraceList = new ArrayList<Trackandtrace>();
		List<TrackingResults> trackingData = auTrackingDetails.getTracking_results();
		ResponseMessage responseMsg =  new ResponseMessage();
		if(trackingData.isEmpty()) {
			responseMsg.setResponseMessage("No Data from AUPost");
		}else {
			SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			for(TrackingResults data : trackingData ) {
				if(data!=null && data.getTrackable_items()!=null) {
					for(TrackableItems trackingLabel : data.getTrackable_items()) {
					    Date latestTime = null;
						try {
							latestTime = output.parse(map.get(trackingLabel.getArticle_id()));
						} catch (ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						if(trackingLabel != null && trackingLabel.getEvents() != null) {
							for(TrackingEvents trackingEvents: trackingLabel.getEvents()) {
							    Date eventTime = null;
								try {
									eventTime = inputFormat.parse(trackingEvents.getDate().substring(0,19));
								} catch (ParseException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								if(eventTime.after(latestTime)) {
								/*System.out.println("Inserting..." + trackingLabel.getArticle_id() +" : "+trackingEvents.getDate().substring(0,19)
										+ " : "+trackingEvents.getDescription());*/
								Trackandtrace trackandTrace = new Trackandtrace();
								//trackandTrace.setRowId(D2ZCommonUtil.generateTrackID());
								trackandTrace.setArticleID(trackingLabel.getArticle_id());
								trackandTrace.setTrackEventDetails(trackingEvents.getDescription());
								trackandTrace.setTrackEventDateOccured(trackingEvents.getDate().substring(0,19));
								trackandTrace.setLocation(trackingEvents.getLocation());
								trackandTrace.setTimestamp(Timestamp.valueOf(LocalDateTime.now()).toString());
							//	trackandTrace.setFileName("AU-Post");
								trackandTrace.setFileName("AUPostTrack");
								trackAndTraceList.add(trackandTrace);
								}
							}
						}
					}
				}
			}
			trackAndTraceRepository.saveAll(trackAndTraceList);
			responseMsg.setResponseMessage("Data uploaded successfully from AU Post");
		}
		return responseMsg;
	}
	
	@Override
	public void logAUPostResponse(List<AUPostResponse> aupostresponse)
	{
		aupostresponseRepository.saveAll(aupostresponse);
	}

	@Override
	public void updateCubicWeight() {
		senderDataRepository.updateCubicWeight();		
	}

	@Override
	public void updateRates() {
		senderDataRepository.updateRates();
	}

	@Override
	public int fetchUserIdbyUserName(String userName) {
		return userRepository.fetchUserIdbyUserName(userName);
	}

	@Override
	public List<SenderdataMaster> fetchDataBasedonSupplier(List<String> incomingRefNbr, String supplier) {
		return senderDataRepository.fetchDataBasedonSupplier(incomingRefNbr, supplier);
	}

	@Override
	public List<String> fetchDataForEtowerForeCastCall(String[] refNbrs) {
		return senderDataRepository.fetchDataForEtowerForeCastCall(refNbrs);
	}

	@Override
	public List<FastwayPostcode> fetchFWPostCodeZone() {
		List<FastwayPostcode> postCodeFWZoneList= (List<FastwayPostcode>) fastwayPostcodeRepository.findAll();
    	return postCodeFWZoneList;
	}

	@Override
	public List<String> fetchDataforPFLSubmitOrder(String[] refNbrs) {
		return senderDataRepository.fetchDataforPFLSubmitOrder(refNbrs);
	}

	@Override
	public String fetchUserById(int userId) {
		return userRepository.fetchUserById( userId);
	}

	@Override

	public List<String> getArticleIDForFreiPostTracking() {
		return trackAndTraceRepository.getArticleIDForFreiPostTracking();
	}
	
	public String createEnquiry(List<CreateEnquiryRequest> createEnquiry) throws ReferenceNumberNotUniqueException {
		List<CSTickets> csTctList = new ArrayList<CSTickets>();
		for(CreateEnquiryRequest enquiryRequest:createEnquiry) {
			CSTickets tickets = new CSTickets();
			String incSeqId = "INC"+D2ZCommonUtil.getday()+csticketsRepository.fetchNextSeq().toString();
			if(enquiryRequest.getType().equalsIgnoreCase("Article Id")) {
				SenderdataMaster senderArticleId = senderDataRepository.fetchDataArticleId(enquiryRequest.getIdentifier());
				if(null != senderArticleId) {
					tickets.setArticleID(senderArticleId.getArticleId());
					tickets.setReferenceNumber(senderArticleId.getReference_number());
					tickets.setConsigneeName(senderArticleId.getConsignee_name());
					tickets.setConsigneeaddr1(senderArticleId.getConsignee_addr1());
					tickets.setConsigneeSuburb(senderArticleId.getConsignee_Suburb());
					tickets.setConsigneeState(senderArticleId.getConsignee_State());
					tickets.setConsigneePostcode(senderArticleId.getConsignee_Postcode());
					tickets.setProductDescription(senderArticleId.getProduct_Description());
					tickets.setCarrier(senderArticleId.getCarrier());
				}
			}else if(enquiryRequest.getType().equalsIgnoreCase("Reference Number")) {
				SenderdataMaster senderRefId = senderDataRepository.fetchDataReferenceNum(enquiryRequest.getIdentifier());
				if(null != senderRefId) {
					tickets.setArticleID(senderRefId.getArticleId());
					tickets.setReferenceNumber(senderRefId.getReference_number());
					tickets.setConsigneeName(senderRefId.getConsignee_name());
					tickets.setConsigneeaddr1(senderRefId.getConsignee_addr1());
					tickets.setConsigneeSuburb(senderRefId.getConsignee_Suburb());
					tickets.setConsigneeState(senderRefId.getConsignee_State());
					tickets.setConsigneePostcode(senderRefId.getConsignee_Postcode());
					tickets.setProductDescription(senderRefId.getProduct_Description());
					tickets.setCarrier(senderRefId.getCarrier());
				}
			}
			tickets.setTicketID(incSeqId);
			tickets.setComments(enquiryRequest.getComments());
			tickets.setDeliveryEnquiry(enquiryRequest.getEnquiry());
			tickets.setPod(enquiryRequest.getPod());
			tickets.setStatus("open");
			tickets.setUserId(enquiryRequest.getUserId());
			tickets.setTrackingEventDateOccured(Timestamp.valueOf(LocalDateTime.now()));
			csTctList.add(tickets);
		}
		List<String> incomingRefNbr = csTctList.stream().map(obj -> {
			return obj.getReferenceNumber(); })
				.collect(Collectors.toList());
		isReferenceNumberUniqueUI(incomingRefNbr);
		for(CSTickets csTicket: csTctList) {
			if(null ==  csTicket.getReferenceNumber()) {
				throw new ReferenceNumberNotUniqueException("Reference Number (or) Article Id is not avilable in the system",null);
			}
		}
		csticketsRepository.saveAll(csTctList);
		return "Enquiry created Successfully";
	}
	
	
	public void isReferenceNumberUniqueUI(List<String> incomingRefNbr) throws ReferenceNumberNotUniqueException{
		System.out.println(incomingRefNbr.toString());
		List<String> referenceNumber_DB = csticketsRepository.fetchAllReferenceNumbers();
		referenceNumber_DB.addAll(incomingRefNbr);
		
		System.out.println(referenceNumber_DB);
		List<String> duplicateRefNbr = referenceNumber_DB.stream().collect(Collectors.groupingBy(Function.identity(),     
	              Collectors.counting()))                                             
	          .entrySet().stream()
	          .filter(e -> e.getValue() > 1)                                      
	          .map(e -> e.getKey())                                                  
	          .collect(Collectors.toList());
		
		if(!duplicateRefNbr.isEmpty()) {
			throw new ReferenceNumberNotUniqueException("Reference Number or Article Id must be unique",duplicateRefNbr);
		}
	}

	@Override
	public List<CSTickets> fetchEnquiry(String status, String fromDate, String toDate, String userId) {
		Integer[] userIds = Arrays.stream(userId.split(","))
		        .map(String::trim)
		        .map(Integer::valueOf)
		        .toArray(Integer[]::new);
		List<CSTickets> enquiryDetails = csticketsRepository.fetchEnquiry(status, fromDate, toDate, userIds);
		return enquiryDetails;
	}

	@Override
	public List<CSTickets> fetchCompletedEnquiry(String userId) {
		Integer[] userIds = Arrays.stream(userId.split(","))
		        .map(String::trim)
		        .map(Integer::valueOf)
		        .toArray(Integer[]::new);
		List<CSTickets> enquiryDetails = csticketsRepository.fetchCompletedEnquiry(userIds);
		return enquiryDetails;
	}

	@Override
	public List<Integer> fetchUserId(String userId) {
		List<Integer> userIds = userRepository.getClientId(userId);
		return userIds;
	}

	@Override
	public List<String> fetchReferencenumberByArticleid(List<String> ArticleID) {
		// TODO Auto-generated method stub
		List<String> refnbrs = senderDataRepository.fetchreferencenumberforArticleid(ArticleID);
		return refnbrs;
	}

	@Override
	public void logcurrencyRate() {
		// TODO Auto-generated method stub
		List<CurrencyDetails> currencyList =currencyproxy.currencyRate();
		currencyRepository.deleteAll();
		List<Currency> currencyObjList = new ArrayList<Currency>();
		for(CurrencyDetails currencyValue: currencyList) {
		Currency currencyObj = new Currency();
		currencyObj.setAudCurrencyRate(currencyValue.getAudCurrencyRate().doubleValue());
		currencyObj.setCountry(currencyValue.getCountry());
		currencyObj.setCurrencyCode(currencyValue.getCurrencyCode());
		currencyObj.setLastUpdated(new Date());
		currencyObjList.add(currencyObj);
		}

		currencyRepository.saveAll(currencyObjList);
		
	}

	@Override
	public Double getAudcurrency(String country) {
		// TODO Auto-generated method stub
		return currencyRepository.getaud(country);
	}

	@Override
	public List<SenderdataMaster> fetchDataBasedonrefnbr(List<String> incomingRefNbr) {
		// TODO Auto-generated method stub
		return senderDataRepository.fetchConsignmentsByRefNbr(incomingRefNbr);
	}


	@Override
	public List<SenderdataMaster> fetchConsignmentsByRefNbr(List<String> refNbrs) {
		return senderDataRepository.fetchConsignmentsByRefNbr(refNbrs);
	}

	@Override
	public List<Returns> returnsOutstanding(String fromDate, String toDate, String userId) {
		Integer[] userIds = Arrays.stream(userId.split(",")).map(String::trim).map(Integer::valueOf).toArray(Integer[]::new);
		List<Returns> returnsDetails = new ArrayList<Returns>();
		System.out.println("fromDate  --->"+fromDate);
		System.out.println("toDate----->"+toDate);
		if( fromDate.equals(null)  && toDate.equals(null)) {
			returnsDetails = returnsRepository.fetchOutstandingDetails(fromDate,toDate,userIds);
		}else {
			returnsDetails = returnsRepository.fetchOutstandingCompleteDetails(userIds);
		}
		return returnsDetails;
	}

	@Override
	public List<SenderdataMaster> fetchShipmentDatabyType(List<String> number, List<Integer> listOfClientId,
			String type) {
		List<SenderdataMaster> senderData;
		
		
		
		if(type.equals("articleid"))
		{
			senderData = senderDataRepository.fetchShipmentDatabyArticleId(number, listOfClientId);
		}
		else if (type.equals("barcodelabel"))
				{
			senderData = senderDataRepository.fetchShipmentDatabyBarcode(number, listOfClientId);
				}
			
			
		else
		{
			System.out.print("in else");
			senderData = senderDataRepository.fetchShipmentDatabyReference(number, listOfClientId);
		}
		
		// TODO Auto-generated method stub
		return senderData;
	}

}
