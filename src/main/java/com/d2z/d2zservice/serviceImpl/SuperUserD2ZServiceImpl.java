package com.d2z.d2zservice.serviceImpl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.d2z.d2zservice.dao.ID2ZSuperUserDao;
import com.d2z.d2zservice.entity.NonD2ZData;
import com.d2z.d2zservice.entity.Reconcile;
import com.d2z.d2zservice.entity.ReconcileND;
import com.d2z.d2zservice.entity.SenderdataMaster;
import com.d2z.d2zservice.entity.Trackandtrace;
import com.d2z.d2zservice.entity.User;
import com.d2z.d2zservice.entity.UserService;
import com.d2z.d2zservice.excelWriter.ShipmentDetailsWriter;
import com.d2z.d2zservice.exception.ReferenceNumberNotUniqueException;
import com.d2z.d2zservice.model.ApprovedInvoice;
import com.d2z.d2zservice.model.ArrivalReportFileData;
import com.d2z.d2zservice.model.BrokerList;
import com.d2z.d2zservice.model.BrokerRatesData;
import com.d2z.d2zservice.model.BrokerShipmentList;
import com.d2z.d2zservice.model.D2ZRatesData;
import com.d2z.d2zservice.model.DownloadInvice;
import com.d2z.d2zservice.model.DropDownModel;
import com.d2z.d2zservice.model.InvoiceShipment;
import com.d2z.d2zservice.model.NotBilled;
import com.d2z.d2zservice.model.ReconcileData;
import com.d2z.d2zservice.model.ResponseMessage;
import com.d2z.d2zservice.model.SenderData;
import com.d2z.d2zservice.model.UploadTrackingFileData;
import com.d2z.d2zservice.model.UserDetails;
import com.d2z.d2zservice.model.UserMessage;
import com.d2z.d2zservice.model.ExportDelete;
import com.d2z.d2zservice.model.etower.TrackingEventResponse;
import com.d2z.d2zservice.proxy.ETowerProxy;
import com.d2z.d2zservice.service.ISuperUserD2ZService;
import com.d2z.d2zservice.validation.D2ZValidator;
import com.d2z.d2zservice.model.ExportShipment;

@Service
public class SuperUserD2ZServiceImpl implements ISuperUserD2ZService{

	@Autowired
    private ID2ZSuperUserDao d2zDao;
	
	@Autowired
	ShipmentDetailsWriter shipmentWriter;
	
	@Autowired
	private ETowerProxy proxy;
	
	@Autowired
    private D2ZValidator d2zValidator;

	@Override
	public UserMessage uploadTrackingFile(List<UploadTrackingFileData> fileData){
		UserMessage userMsg = new UserMessage();
		List<Trackandtrace> insertedData = d2zDao.uploadTrackingFile(fileData);
		if(insertedData.isEmpty()) {
			userMsg.setMessage("Failed to upload data");
			return userMsg;
		}
		userMsg.setMessage("Data uploaded successfully");
		return userMsg;
	}

	@Override
	public UserMessage uploadArrivalReport(List<ArrivalReportFileData> fileData) {
		UserMessage userMsg = new UserMessage();
		List<Trackandtrace> insertedData = d2zDao.uploadArrivalReport(fileData);
		if(insertedData.isEmpty()) {
			userMsg.setMessage("Failed to upload data");
			return userMsg;
		}
		userMsg.setMessage("Data uploaded successfully");
		return userMsg;
	}

	@Override
	public List<DropDownModel> brokerCompanyDetails() {
		List<String> listOfCompany = d2zDao.brokerCompanyDetails();
		List<DropDownModel> dropDownList= new ArrayList<DropDownModel>();
		for(String companyName:listOfCompany) {
			DropDownModel dropDownVaL = new DropDownModel();
			dropDownVaL.setName(companyName);
			dropDownVaL.setValue(companyName);
			dropDownList.add(dropDownVaL);
		}
		return dropDownList;
	}

	@Override
	public UserDetails fetchUserDetails(String companyName) {
		User user = d2zDao.fetchUserDetails(companyName);
		UserDetails userDetails = new UserDetails();
		userDetails.setAddress(user.getAddress());
		userDetails.setCompanyName(user.getCompanyName());
		userDetails.setContactName(user.getName());
		userDetails.setContactPhoneNumber(user.getPhoneNumber());
		userDetails.setCountry(user.getCountry());
		userDetails.setEmailAddress(user.getEmail());
		userDetails.setPassword(user.getPassword_value());
		userDetails.setPostCode(user.getPostcode());
		userDetails.setState(user.getState());
		userDetails.setSuburb(user.getSuburb());
		userDetails.setUserName(user.getUsername());
		Set<UserService> userServiceList = user.getUserService();
		List<String> serviceType = userServiceList.stream().map(obj ->{
			return obj.getServiceType();}).collect(Collectors.toList());
		userDetails.setServiceType(serviceType);
		return userDetails;
	}

	@Override
	public List<ExportDelete> exportDeteledConsignments(String fromDate, String toDate) {
		/*return d2zDao.exportDeteledConsignments(fromDate,toDate);*/
		List<ExportDelete> exportdeletelist = new ArrayList<ExportDelete>();
		
		List<String> ExportDeleteList = d2zDao.exportDeteledConsignments(fromDate, toDate);
		Iterator itr = ExportDeleteList.iterator();
		while(itr.hasNext()){
		   Object[] obj = (Object[]) itr.next();
		   //now you have one array of Object for each row
		   
		   ExportDelete exportval = new ExportDelete();
			exportval.setBrokername(String.valueOf(obj[0]));
			exportval.setReference_number(String.valueOf(obj[1]));
			exportval.setBarcodelabelNumber(String.valueOf(obj[2]));
			exportdeletelist.add(exportval);
		
		}
	
		//ExportDeleteList.forEach(System.out::println);
		return exportdeletelist;
		//byte[] bytes = shipmentWriter.generateDeleteConsignmentsxls(deletedConsignments);
		//return bytes;
	}
	@Override
	public List<SenderdataMaster> exportConsignmentData(String fromDate, String toDate) {
		return d2zDao.exportConsignments(fromDate, toDate);
	}
	@Override
	public List<ExportShipment> exportShipmentData(String fromDate, String toDate) {
List<ExportShipment> exportshipmentlist = new ArrayList<ExportShipment>();
		
		List<Object> ExportDeleteList = d2zDao.exportShipment(fromDate, toDate);
	
		Iterator itr = ExportDeleteList.iterator();
		while(itr.hasNext()){
		   Object[] obj = (Object[]) itr.next();
		  // System.out.println(obj.length);
		   //now you have one array of Object for each row
		   
		  
		   ExportShipment exportval = new ExportShipment();
			exportval.setBroker_name(String.valueOf(obj[0]));
			exportval.setReference_number(String.valueOf(obj[1]));
			exportval.setValue(Double.valueOf(""+obj[2]));
			exportval.setShippedQuantity(Integer.valueOf(""+obj[3]));
			exportval.setConsignee_name(String.valueOf(obj[4]));
			exportval.setConsignee_addr1(String.valueOf(obj[5]));
			exportval.setConsignee_Suburb(String.valueOf(obj[6]));
			exportval.setConsignee_State(String.valueOf(obj[7]));
			exportval.setConsignee_Postcode(String.valueOf(obj[8]));
			exportval.setConsignee_Phone(String.valueOf(obj[9]));
			exportval.setProduct_Description(String.valueOf(obj[10]));
			exportval.setShipper_Country(String.valueOf(obj[11]));
			exportval.setWeight(Double.valueOf(""+obj[12]));
			exportval.setBarcodelabelNumber(String.valueOf(obj[13]));
			exportval.setServicetype(String.valueOf(obj[14]));
			exportval.setCurrency(String.valueOf(obj[15]));
			
			
			
			
			
			
		   exportshipmentlist.add(exportval);
		
		}
	
		//ExportDeleteList.forEach(System.out::println);
		return exportshipmentlist;
		}
	@Override
	public List<ExportShipment> exportNonShipmentData(String fromDate, String toDate) {
List<ExportShipment> exportshipmentlist = new ArrayList<ExportShipment>();
		
		List<Object> ExportDeleteList = d2zDao.exportNonShipment(fromDate, toDate);
	
		Iterator itr = ExportDeleteList.iterator();
		while(itr.hasNext()){
		   Object[] obj = (Object[]) itr.next();
		  
		   //now you have one array of Object for each row
		   
		  
		   ExportShipment exportval = new ExportShipment();
			exportval.setBroker_name(String.valueOf(obj[0]));
			exportval.setReference_number(String.valueOf(obj[1]));
			exportval.setValue(Double.valueOf(""+obj[2]));
			exportval.setShippedQuantity(Integer.valueOf(""+obj[3]));
			exportval.setConsignee_name(String.valueOf(obj[4]));
			exportval.setConsignee_addr1(String.valueOf(obj[5]));
			exportval.setConsignee_Suburb(String.valueOf(obj[6]));
			exportval.setConsignee_State(String.valueOf(obj[7]));
			exportval.setConsignee_Postcode(String.valueOf(obj[8]));
			exportval.setConsignee_Phone(String.valueOf(obj[9]));
			exportval.setProduct_Description(String.valueOf(obj[10]));
			exportval.setShipper_Country(String.valueOf(obj[11]));
			exportval.setWeight(Double.valueOf(""+obj[12]));
			exportval.setBarcodelabelNumber(String.valueOf(obj[13]));
			exportval.setServicetype(String.valueOf(obj[14]));
			exportval.setCurrency(String.valueOf(obj[15]));
			
			
			
			
			
			
		   exportshipmentlist.add(exportval);
		
		}
	
		//ExportDeleteList.forEach(System.out::println);
		return exportshipmentlist;
	
		}
	@Override
	public ResponseMessage trackingEvent(List<String> trackingNbrs) {
		ResponseMessage respMsg = null;
		List<List<String>> trackingNbrList = ListUtils.partition(trackingNbrs, 300);
		int count = 1;
		for(List<String> trackingNumbers : trackingNbrList) {
			System.out.println(count + ":::" + trackingNumbers.size());
			count++;
		TrackingEventResponse response = proxy.makeCallForTrackingEvents(trackingNumbers);
		respMsg = d2zDao.insertTrackingDetails(response);
     	}
		//List<List<ETowerTrackingDetails>> response = proxy.stubETower();
		return respMsg;
	}

	//@Scheduled(cron = "0 0 0/2 * * ?")
//	@Scheduled(cron = "0 0/10 * * * ?")
	public void scheduledTrackingEvent() {
		List<String> trackingNumbers = d2zDao.fetchTrackingNumbersForETowerCall();
		if(trackingNumbers.isEmpty()) {
			System.out.println("ETower call not required");
		}
		else {
			trackingEvent(trackingNumbers);
		}
	}

	@Override
	public UserMessage uploadBrokerRates(List<BrokerRatesData> brokerRatesData) {
		String uploadInvoiceData = d2zDao.uploadBrokerRates(brokerRatesData);
		UserMessage userMsg = new UserMessage();
		userMsg.setMessage(uploadInvoiceData);
		return userMsg;
		}

	@Override
	public UserMessage uploadD2ZRates(List<D2ZRatesData> d2zRatesData) {
		String uploadedD2ZRates = d2zDao.uploadD2ZRates(d2zRatesData);
		UserMessage userMsg = new UserMessage();
		userMsg.setMessage(uploadedD2ZRates);
		return userMsg;
	}

	@Override
	public List<BrokerList> brokerList() {
		List<User> brokerList = d2zDao.brokerList();
		List<BrokerList> brokerListDetails = new ArrayList<BrokerList>();
		brokerList.forEach(users -> {
			BrokerList broker = new BrokerList();
			DropDownModel dropDownBroker  = new DropDownModel();
			dropDownBroker.setName(users.getUsername());
			dropDownBroker.setValue(users.getUsername());
			broker.setBrokerUserName(dropDownBroker);
			broker.setUserId(users.getUser_Id());
			Set<DropDownModel> dropDownServiceList  = new HashSet<DropDownModel>();
			Set<DropDownModel> dropDownTypeList  = new HashSet<DropDownModel>();
			users.getUserService().forEach(service -> {
				if(service.getServiceType() != null) {
					DropDownModel dropDownService  = new DropDownModel();
					dropDownService.setName(service.getServiceType());
					dropDownService.setValue(service.getServiceType());
					dropDownServiceList.add(dropDownService);
					broker.setServiceType(dropDownServiceList);
				}
				if(service.getInjectionType() != null) {
					DropDownModel dropDownType  = new DropDownModel();
					dropDownType.setName(service.getInjectionType());
					dropDownType.setValue(service.getInjectionType());
					dropDownTypeList.add(dropDownType);
					broker.setInjectionType(dropDownTypeList);
				}
			});
			brokerListDetails.add(broker);
		});

		return brokerListDetails;
	}
	
	@Override
	public List<DropDownModel> fetchMlidList() {
		List<String> listOfMlid = d2zDao.fetchMlidList();
		List<DropDownModel> mlidList= new ArrayList<DropDownModel>();
		for(String mlid:listOfMlid) {
			DropDownModel dropDownVaL = new DropDownModel();
			dropDownVaL.setName(mlid);
			dropDownVaL.setValue(mlid);
			mlidList.add(dropDownVaL);
		}
		return mlidList;
	}

	@Override
	public List<BrokerShipmentList> brokerShipmentList() {
		List<User> brokerList = d2zDao.brokerList();
		List<BrokerShipmentList> brokerShipmentData = new ArrayList<BrokerShipmentList>();
		brokerList.forEach(broker -> {
			BrokerShipmentList brokerShipment = new BrokerShipmentList();
			DropDownModel dropDownBroker  = new DropDownModel();
			dropDownBroker.setName(broker.getUsername());
			dropDownBroker.setValue(broker.getUsername());
			List<String> brokerShipmentList = d2zDao.brokerShipmentList(broker.getUser_Id());
			Set<DropDownModel> shipmentList= new HashSet<DropDownModel>();
			brokerShipmentList.forEach(shipment -> {
				if(null != shipment) {
					DropDownModel dropDownVaL = new DropDownModel();
					dropDownVaL.setName(shipment);
					dropDownVaL.setValue(shipment);
					shipmentList.add(dropDownVaL);
				}
			});
			brokerShipment.setBrokerUserName(dropDownBroker);
			brokerShipment.setUserId(broker.getUser_Id());
			brokerShipment.setShipmentNumber(shipmentList);
			brokerShipmentData.add(brokerShipment);
		});
		return brokerShipmentData;
	}

	@Override
	public List<InvoiceShipment> brokerShipment() {
		List<String> brokerShipmentList = d2zDao.brokerShipment();
		List<InvoiceShipment> invoiceShipmentList = new ArrayList<InvoiceShipment>();
		Iterator itr = brokerShipmentList.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 InvoiceShipment invoiceShipment = new InvoiceShipment();
			 if(obj[0] != null)
				 invoiceShipment.setBrokerName(obj[0].toString());
			 if(obj[1] != null)
				 invoiceShipment.setShipmentNumber(obj[1].toString());
			 invoiceShipmentList.add(invoiceShipment);
       }
		return invoiceShipmentList;
	}

	@Override
	public List<InvoiceShipment> brokerInvoiced() {
		List<String> brokerShipmentList = d2zDao.brokerInvoiced();
		List<InvoiceShipment> invoiceShipmentList = new ArrayList<InvoiceShipment>();
		Iterator itr = brokerShipmentList.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 InvoiceShipment invoiceShipment = new InvoiceShipment();
			 if(obj[0] != null)
				 invoiceShipment.setBrokerName(obj[0].toString());
			 if(obj[1] != null)
				 invoiceShipment.setShipmentNumber(obj[1].toString());
			 invoiceShipmentList.add(invoiceShipment);
       }
		return invoiceShipmentList;
	}

	@Override
	public UserMessage fetchReconcile(List<ReconcileData> reconcileData) throws ReferenceNumberNotUniqueException {
		List<Reconcile> reconcileCalculatedList = new ArrayList<Reconcile>();
		List<String> reconcileReferenceNum = new ArrayList<String>();
		//Check for duplicates
		if(reconcileData.get(0).getSupplierType().equalsIgnoreCase("freipost")) {
			d2zValidator.isReferenceNumberUniqueReconcile(reconcileData,"D2Z");
		}else{
			d2zValidator.isArticleIdUniqueReconcile(reconcileData,"D2Z");
		}
		reconcileData.forEach(reconcile -> {
			List<String> reconcileList = d2zDao.reconcileData(reconcile.getArticleNo(), reconcile.getRefrenceNumber());
			Reconcile reconcileObj = new Reconcile();
			Iterator reconcileItr = reconcileList.iterator();
			 while(reconcileItr.hasNext()) {
				 Object[] obj = (Object[]) reconcileItr.next();
				 MathContext mc = new MathContext(2);
				 if(obj[0] != null)
					 reconcileObj.setBrokerUserName(obj[0].toString());
				 if(obj[1] != null)
					 reconcileObj.setAirwaybill(obj[1].toString());
				 if(obj[2] != null)
					 reconcileObj.setArticleId(obj[2].toString());
				 if(obj[3] != null)
					 reconcileObj.setReference_number(obj[3].toString());
				 if(obj[4] != null)
					 reconcileObj.setD2ZCost(new BigDecimal(obj[4].toString()));
				 if(obj[5] != null)
					 reconcileObj.setD2ZWeight(Double.parseDouble(obj[5].toString()));
				 if(obj[6] != null)
					 reconcileObj.setInvoicedAmount(new BigDecimal(obj[6].toString()));
				 if(reconcileData.get(0).getSupplierType().equalsIgnoreCase("UBI")) {
					 reconcileObj.setSupplierCharge(BigDecimal.valueOf(reconcile.getNormalRateParcel()));
					 reconcileObj.setSupplierWeight(reconcile.getArticleActualWeight());
					 reconcileObj.setWeightDifference((reconcileObj.getSupplierWeight()) - (reconcileObj.getD2ZWeight()));
				 }else if(reconcileData.get(0).getSupplierType().equalsIgnoreCase("freipost")) {
					 reconcileObj.setSupplierCharge(BigDecimal.valueOf(reconcile.getCost()));
					 reconcileObj.setSupplierWeight(reconcile.getChargedWeight());
					 reconcileObj.setWeightDifference((reconcileObj.getSupplierWeight()) - (reconcileObj.getD2ZWeight()));
				 }
				 if(reconcileObj.getSupplierCharge() != null && reconcileObj.getD2ZCost() != null)
					 reconcileObj.setCostDifference(reconcileObj.getSupplierCharge().subtract(reconcileObj.getD2ZCost(), mc));
				 reconcileObj.setSupplierType(reconcileData.get(0).getSupplierType());
			 }
			 reconcileCalculatedList.add(reconcileObj);
			 if(reconcileObj.getBrokerUserName() != null ) {
				 reconcileReferenceNum.add(reconcileObj.getReference_number());
			 }else {
				 if(reconcileData.get(0).getSupplierType().equalsIgnoreCase("UBI")) {
					 reconcileObj.setArticleId(reconcile.getArticleNo());
				}else if(reconcileData.get(0).getSupplierType().equalsIgnoreCase("freipost")){
					 reconcileObj.setReference_number(reconcile.getRefrenceNumber());
				}
			 }
			 reconcileCalculatedList.add(reconcileObj);
		});
			d2zDao.reconcileUpdate(reconcileCalculatedList);
			if(!reconcileReferenceNum.isEmpty())
				d2zDao.reconcilerates(reconcileReferenceNum);
			UserMessage userMsg = new UserMessage();
			userMsg.setMessage("Reconcile data uploaded successfully, Please click download button to explore the data");
			return userMsg;
	}

	@Override
	public UserMessage approvedInvoice(com.d2z.d2zservice.model.ApprovedInvoice approvedInvoice) {
		//Calling Delete Store Procedure
		UserMessage approvedInvoiceMsg= d2zDao.approvedInvoice(approvedInvoice);
		return approvedInvoiceMsg;
	}

	@Override
	public List<NotBilled> fetchNotBilled() {
		List<String> notBilledData = d2zDao.fetchNotBilled();
		List<NotBilled> notBilledList = new ArrayList<NotBilled>();
		Iterator itr = notBilledData.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 NotBilled notBilled = new NotBilled();
			 if(obj[0] != null)
				 notBilled.setUserName(obj[0].toString());
			 if(obj[1] != null)
				 notBilled.setAirwayBill(obj[1].toString());
			 if(obj[2] != null)
				 notBilled.setArticleId(obj[2].toString());
			 if(obj[3] != null)
				 notBilled.setReferenceNumber(obj[3].toString());
			 if(obj[4] != null)
				 notBilled.setD2zRate(Double.parseDouble(obj[4].toString()));
			 notBilledList.add(notBilled);
       }
		return notBilledList;
	}

	@Override
	public List<DownloadInvice> downloadInvoice(List<String> broker, List<String> airwayBill, String billed, String invoiced) {
		List<String> downloadInvoiceData = d2zDao.downloadInvoice(broker,airwayBill,billed,invoiced);
		List<DownloadInvice> downloadInvoiceList = new ArrayList<DownloadInvice>();
		Iterator itr = downloadInvoiceData.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 DownloadInvice downloadInvoice = new DownloadInvice();
			 if(obj[0] != null)
				 downloadInvoice.setTrackingNumber(obj[0].toString());
			 if(obj[1] != null)
				 downloadInvoice.setReferenceNuber(obj[1].toString());
			 if(obj[2] != null)
				 downloadInvoice.setPostcode(obj[2].toString());
			 if(obj[3] != null)
				 downloadInvoice.setWeight(obj[3].toString());
			 if(obj[4] != null)
				 downloadInvoice.setPostage(obj[4].toString());
			 if(obj[5] != null)
				 downloadInvoice.setFuelsurcharge(obj[5].toString());
			 if(obj[6] != null)
				 downloadInvoice.setTotal(obj[6].toString());
			 if(obj[7] != null)
				 downloadInvoice.setServiceType(obj[7].toString());
			 downloadInvoiceList.add(downloadInvoice);
       }
		return downloadInvoiceList;
	}

	@Override
	public UserMessage fetchNonD2zClient(List<NonD2ZData> nonD2zData) throws ReferenceNumberNotUniqueException {
		d2zValidator.isArticleIdUniqueUI(nonD2zData);
		UserMessage uploadNonD2zClient= d2zDao.fetchNonD2zClient(nonD2zData);
		return uploadNonD2zClient;
	}

	@Override
	public List<Reconcile> downloadReconcile(List<String> reconcileNumbers) {
		List<Reconcile> reconcileData = d2zDao.downloadReconcile(reconcileNumbers);
		return reconcileData;
	}

	@Override
	public List<DropDownModel> fetchNonD2zBrokerUserName() {
		List<String> listOffBrokerName= d2zDao.fetchNonD2zBrokerUserName();
		List<DropDownModel> dropDownList= new ArrayList<DropDownModel>();
		for(String brokerName:listOffBrokerName) {
			DropDownModel dropDownVaL = new DropDownModel();
			dropDownVaL.setName(brokerName);
			dropDownVaL.setValue(brokerName);
			dropDownList.add(dropDownVaL);
		}
		return dropDownList;
	}

	@Override
	public UserMessage uploadReconcileNonD2z(List<ReconcileData> reconcileNonD2zData) throws ReferenceNumberNotUniqueException{
		List<ReconcileND> reconcileNoND2zList = new ArrayList<ReconcileND>();
		List<String> reconcileArticleIdNum = new ArrayList<String>();
		if(reconcileNonD2zData.get(0).getSupplierType().equalsIgnoreCase("freipost")) {
			d2zValidator.isReferenceNumberUniqueReconcile(reconcileNonD2zData,"ND2Z");
		}else{
			d2zValidator.isArticleIdUniqueReconcile(reconcileNonD2zData,"ND2Z");
		}
		reconcileNonD2zData.forEach(reconcileNonD2z -> {
			NonD2ZData reconcileObj = null;
			if(reconcileNonD2z.getSupplierType().equalsIgnoreCase("freipost")) {
				reconcileObj = d2zDao.reconcileNonD2zFreipostData(reconcileNonD2z.getRefrenceNumber());
			}else {
				reconcileObj = d2zDao.reconcileNonD2zData(reconcileNonD2z.getArticleNo());
			}
			ReconcileND reconcileNonD2Obj = new ReconcileND();
			MathContext mc = new MathContext(2);
			if(reconcileObj != null){
				reconcileNonD2Obj.setBrokerUserName(reconcileObj.getBrokerName());
				reconcileNonD2Obj.setAirwaybill(reconcileObj.getShipmentNumber());
				reconcileNonD2Obj.setArticleId(reconcileObj.getArticleId());
				reconcileNonD2Obj.setReference_number(reconcileObj.getReference_number());
					reconcileNonD2Obj.setSupplierCharge(BigDecimal.valueOf(reconcileNonD2z.getCost()));
				if(reconcileObj.getD2Zrate() != null)
					reconcileNonD2Obj.setD2ZCost(new BigDecimal(reconcileObj.getD2Zrate()));
				if(reconcileNonD2Obj.getSupplierCharge() != null && reconcileNonD2Obj.getD2ZCost() != null)
					reconcileNonD2Obj.setCostDifference(reconcileNonD2Obj.getSupplierCharge().subtract(reconcileNonD2Obj.getD2ZCost(), mc));
				reconcileNonD2Obj.setSupplierWeight(reconcileNonD2z.getChargedWeight());
				reconcileNonD2Obj.setD2ZWeight(reconcileObj.getWeight());
				reconcileNonD2Obj.setWeightDifference((reconcileNonD2Obj.getSupplierWeight()) - (reconcileNonD2Obj.getD2ZWeight()));
				if(reconcileObj.getBrokerRate() != null)
					reconcileNonD2Obj.setInvoicedAmount(new BigDecimal(reconcileObj.getBrokerRate()));
				reconcileNonD2Obj.setSupplierType(reconcileNonD2z.getSupplierType());
				reconcileArticleIdNum.add(reconcileObj.getArticleId());
			}else{
				if(reconcileNonD2z.getSupplierType().equalsIgnoreCase("freipost")) {
					reconcileNonD2Obj.setReference_number(reconcileNonD2z.getRefrenceNumber());
				}else {
					reconcileNonD2Obj.setArticleId(reconcileNonD2z.getArticleNo());
				}
			}
			reconcileNoND2zList.add(reconcileNonD2Obj);
		});
			d2zDao.reconcileNonD2zUpdate(reconcileNoND2zList);
			if(!reconcileArticleIdNum.isEmpty())
				d2zDao.reconcileratesND(reconcileArticleIdNum);
			UserMessage userMsg = new UserMessage();
			userMsg.setMessage("Reconcile data uploaded successfully, Please click download button to explore the data");
			return userMsg;
	}

	@Override
	public List<ReconcileND> downloadNonD2zReconcile(List<String> nonD2zReconcileNumbers) {
		List<ReconcileND> nonD2zReconcileData = d2zDao.downloadNonD2zReconcile(nonD2zReconcileNumbers);
		return nonD2zReconcileData;
	}

	@Override
	public List<InvoiceShipment> brokerNonD2zShipment() {
		List<NonD2ZData> pendingNonD2zInvoice = d2zDao.brokerNonD2zShipment();
		List<InvoiceShipment> invoiceShipmentList = new ArrayList<InvoiceShipment>();
		Iterator itr = pendingNonD2zInvoice.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 InvoiceShipment invoiceShipment = new InvoiceShipment();
			 if(obj[0] != null)
				 invoiceShipment.setBrokerName(obj[0].toString());
			 if(obj[1] != null)
				 invoiceShipment.setShipmentNumber(obj[1].toString());
			 invoiceShipmentList.add(invoiceShipment);
       }
		return invoiceShipmentList;
	}

	@Override
	public List<DownloadInvice> downloadNonD2zInvoice(List<String> broker,List<String> airwayBill,String billed,String invoiced) {
		List<String> downloadNonD2zInvoiceData = d2zDao.downloadNonD2zInvoice(broker,airwayBill,billed,invoiced);
		List<DownloadInvice> downloadNonD2zInvoiceList = new ArrayList<DownloadInvice>();
		Iterator itr = downloadNonD2zInvoiceData.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 DownloadInvice downloadInvoice = new DownloadInvice();
			 if(obj[0] != null)
				 downloadInvoice.setTrackingNumber(obj[0].toString());
			 if(obj[1] != null)
				 downloadInvoice.setReferenceNuber(obj[1].toString());
			 if(obj[2] != null)
				 downloadInvoice.setPostcode(obj[2].toString());
			 if(obj[3] != null)
				 downloadInvoice.setWeight(obj[3].toString());
			 if(obj[4] != null)
				 downloadInvoice.setPostage(obj[4].toString());
			 if(obj[5] != null)
				 downloadInvoice.setFuelsurcharge(obj[5].toString());
			 if(obj[6] != null)
				 downloadInvoice.setTotal(obj[6].toString());
			 if(obj[7] != null)
				 downloadInvoice.setServiceType(obj[7].toString());
			 downloadNonD2zInvoiceList.add(downloadInvoice);
       }
		return downloadNonD2zInvoiceList;
	}

	@Override
	public UserMessage approveNdInvoiced(ApprovedInvoice approvedInvoice) {
	//Calling Delete Store Procedure
	UserMessage approvedInvoiceMsg= d2zDao.approveNdInvoiced(approvedInvoice);
	return approvedInvoiceMsg;
	}

	@Override
	public List<InvoiceShipment> brokerNdInvoiced() {
		List<NonD2ZData> approvedNonD2zInvoice = d2zDao.brokerNdInvoiced();
		List<InvoiceShipment> approvedShipmentList = new ArrayList<InvoiceShipment>();
		Iterator itr = approvedNonD2zInvoice.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 InvoiceShipment invoiceShipment = new InvoiceShipment();
			 if(obj[0] != null)
				 invoiceShipment.setBrokerName(obj[0].toString());
			 if(obj[1] != null)
				 invoiceShipment.setShipmentNumber(obj[1].toString());
			 approvedShipmentList.add(invoiceShipment);
       }
		return approvedShipmentList;
	}

	@Override
	public List<NotBilled> fetchNonD2zNotBilled() {
		List<String> notBilledData = d2zDao.fetchNonD2zNotBilled();
		List<NotBilled> notBilledList = new ArrayList<NotBilled>();
		Iterator itr = notBilledData.iterator();
		 while(itr.hasNext()) {
			 Object[] obj = (Object[]) itr.next();
			 NotBilled notBilled = new NotBilled();
			 if(obj[0] != null)
				 notBilled.setUserName(obj[0].toString());
			 if(obj[1] != null)
				 notBilled.setAirwayBill(obj[1].toString());
			 if(obj[2] != null)
				 notBilled.setArticleId(obj[2].toString());
			 if(obj[3] != null)
				 notBilled.setReferenceNumber(obj[3].toString());
			 if(obj[4] != null)
				 notBilled.setD2zRate(Double.parseDouble(obj[4].toString()));
			 notBilledList.add(notBilled);
       }
		return notBilledList;
	}

}
