package com.d2z.d2zservice.controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d2z.d2zservice.entity.CSTickets;
import com.d2z.d2zservice.entity.Returns;
import com.d2z.d2zservice.entity.SenderdataMaster;
import com.d2z.d2zservice.exception.EtowerFailureResponseException;
import com.d2z.d2zservice.exception.PCAlabelException;
import com.d2z.d2zservice.exception.ReferenceNumberNotUniqueException;
import com.d2z.d2zservice.model.ClientDashbaord;
import com.d2z.d2zservice.model.CreateEnquiryRequest;
import com.d2z.d2zservice.model.DropDownModel;
import com.d2z.d2zservice.model.Ebay_ShipmentDetails;
import com.d2z.d2zservice.model.ResponseMessage;
import com.d2z.d2zservice.model.ReturnsClientResponse;
import com.d2z.d2zservice.model.SenderData;
import com.d2z.d2zservice.model.SenderDataResponse;
import com.d2z.d2zservice.model.ShipmentDetails;
import com.d2z.d2zservice.model.TrackParcel;
import com.d2z.d2zservice.model.TrackingDetails;
import com.d2z.d2zservice.model.UserDetails;
import com.d2z.d2zservice.model.UserMessage;
import com.d2z.d2zservice.service.ID2ZService;
import com.d2z.d2zservice.service.ISuperUserD2ZService;

@RestController
@Validated
@RequestMapping(value = "/v1/d2z")
public class D2zController {

	Logger logger = LoggerFactory.getLogger(D2zController.class);

	@Autowired
	private ID2ZService d2zService;
	
	@Autowired
	private ISuperUserD2ZService superUserD2zService;

	@RequestMapping(method = RequestMethod.GET, path = "/login")
	public UserDetails login(@RequestParam("userName") String userName, @RequestParam("passWord") String passWord) {
		UserDetails userDetails = d2zService.login(userName, passWord);
		return userDetails;
    }
	
	@RequestMapping( method = RequestMethod.POST, path = "/consignment-fileUpload")
    public List<SenderDataResponse> consignmentFileUpload( @RequestBody List<@Valid SenderData> orderDetailList) throws ReferenceNumberNotUniqueException, EtowerFailureResponseException{
		List<SenderDataResponse> successMsg = d2zService.exportParcel(orderDetailList);
		 Runnable r = new Runnable( ) {			
		        public void run() {
		        	
		        	List<String> incomingRefNbr = orderDetailList.stream().map(obj -> {
		    			return obj.getReferenceNumber(); })
		    				.collect(Collectors.toList());
		        	d2zService.makeCallToEtowerBasedonSupplierUI(incomingRefNbr);
		    		
		        }
		     };
		    new Thread(r).start();

		return successMsg;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/consignment-fileList")
	public List<DropDownModel> fileList(@RequestParam("userId") Integer userId) {
		List<DropDownModel> fileList = d2zService.fileList(userId);
		return fileList;
	}
	
	@RequestMapping(method = RequestMethod.POST, path = "/create-enquiry")
	public UserMessage createEnquiry(@RequestBody List<CreateEnquiryRequest> createEnquiry) throws ReferenceNumberNotUniqueException {
		UserMessage enquiryInfo = d2zService.createEnquiry(createEnquiry);
		return enquiryInfo;
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/enquiry")
	public List<CSTickets> fetchEnquiry(@RequestParam("status") String status, @RequestParam("fromDate") String fromDate, 
			@RequestParam("toDate") String toDate, @RequestParam("userId") String userId) {
		List<CSTickets> enquiryInfo = d2zService.fetchEnquiry(status, fromDate, toDate, userId);
		return enquiryInfo;
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/completed-Enquiry")
	public List<CSTickets> fetchCompletedEnquiry(@RequestParam("userId") String userId) {
		List<CSTickets> completedEnquiry = d2zService.fetchCompletedEnquiry(userId);
		return completedEnquiry;
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/userId")
	public List<Integer> fetchUserId(@RequestParam("userId") String userId) {
		List<Integer> userIds = d2zService.fetchUserId(userId);
		return userIds;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/label-fileList")
	public List<DropDownModel> labelFileList(@RequestParam("userId") Integer userId) {
		List<DropDownModel> fileList = d2zService.labelFileList(userId);
		return fileList;
	}

	@RequestMapping(method = RequestMethod.POST, path = "/consignment-fileData")
	public List<SenderdataMaster> consignmentFileData(@RequestBody String fileName) {
		List<SenderdataMaster> fileListDataLabel = d2zService.consignmentFileData(fileName);
		return fileListDataLabel;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/manifest-data")
	public List<SenderdataMaster> fetchManifestData(@RequestParam("fileName") String fileName) {
		List<SenderdataMaster> fileListData = d2zService.fetchManifestData(fileName);
		return fileListData;
	}

	@RequestMapping(method = RequestMethod.POST, path = "/consignment-delete")
	public UserMessage consignmentDelete(@RequestBody String refrenceNumlist) {
		UserMessage fileDeleteMsg = d2zService.consignmentDelete(refrenceNumlist);
		return fileDeleteMsg;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/manifest-creation")
	public UserMessage manifestCreation(@RequestParam("manifestNumber") String manifestNumber,
			@RequestParam("refrenceNumber") String refrenceNumber) {
		UserMessage manifestUpdateMsg = d2zService.manifestCreation(manifestNumber, refrenceNumber);
		return manifestUpdateMsg;
	}

	@RequestMapping(method = RequestMethod.POST, path = "/generateLabel")
	public ResponseEntity<byte[]> generateLabel(@RequestBody List<SenderData> senderData) {
		byte[] bytes = d2zService.generateLabel(senderData);
		System.out.println("Byte Response ----->");
		System.out.println(bytes.toString());
		return ResponseEntity.ok()
				// Specify content type as PDF
				.header("Content-Type", "application/pdf; charset=UTF-8")
				// Tell browser to display PDF if it can
				.header("Content-Disposition", "inline; filename=\"Label.pdf\"").body(bytes);
	}

	@RequestMapping(method = RequestMethod.GET, path = "/tracking-list")
	public List<TrackingDetails> trackingDetails(@RequestParam("fileName") String fileName) {
		List<TrackingDetails> trackingDetails = d2zService.trackingDetails(fileName);
		return trackingDetails;
	}

	@RequestMapping(method = RequestMethod.POST, path = "/tracking-label")
	public ResponseEntity<byte[]> trackingLabel(@RequestBody String refBarNum) throws PCAlabelException{
		System.out.println("Incoming refBarNum :: " + refBarNum);
		List<String> refBarNumArray = Stream.of(refBarNum.split(",")).collect(Collectors.toList());
		byte[] bytes = d2zService.trackingLabel(refBarNumArray);
		return ResponseEntity.ok()
				// Specify content type as PDF
				.header("Content-Type", "application/pdf; charset=UTF-8")
				// Tell browser to display PDF if it can
				.header("Content-Disposition", "inline; filename=\"Label.pdf\"").body(bytes);
	}

	@RequestMapping(method = RequestMethod.GET, path = "/trackParcel/referenceNumber/{referenceNumbers}")
	public List<TrackParcel> trackParcel(@PathVariable List<String> referenceNumbers) {
		List<TrackParcel> trackParcelResponse = d2zService.trackParcelByRefNbr(referenceNumbers);
		return trackParcelResponse;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/trackParcel/articleID/{articleIDs}")
	public List<TrackParcel> trackParcelByArticleID(@PathVariable List<String> articleIDs) {
		List<TrackParcel> trackParcelResponse = d2zService.trackParcelByArticleID(articleIDs);
		return trackParcelResponse;
	}

	@RequestMapping(method = RequestMethod.POST, path = "/user")
	public UserMessage addUser(@Valid @RequestBody UserDetails userDetails) {
		return d2zService.addUser(userDetails);
	}

	@RequestMapping(method = RequestMethod.GET, path = "/userservice")
	public UserMessage addUserservice( @RequestParam("userName")String userName, @RequestParam ("serviceType")String serviceType ) {
		return d2zService.addUserService(userName,serviceType);
	}
	@RequestMapping(method = RequestMethod.PUT, path = "/user")
	public UserMessage updateUser(@Valid @RequestBody UserDetails userDetails) {
		UserMessage userMsg = d2zService.updateUser(userDetails);
		return userMsg;
	}

	@RequestMapping(method = RequestMethod.PUT, path = "/user/delete/{companyName}")
	public UserMessage deleteUser(@PathVariable String companyName, @PathVariable String roleId) {
		UserMessage userMsg = d2zService.deleteUser(companyName, roleId);
		return userMsg;
	}
	
//	@RequestMapping( method = RequestMethod.GET, path = "/consignments/shipment")
//    public ResponseEntity<byte[]> downloadShipmentData(@RequestParam("shipmentNumber") String shipmentNumber) {
//    	byte[] bytes = d2zService.downloadShipmentData(shipmentNumber);
//    	 String fileName = shipmentNumber+"_"+Timestamp.from(Instant.now())+".xlsx";
//    	
//    	 return ResponseEntity
//    		      .ok()
//    		      .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
//    		      // Tell browser to display PDF if it can
//    		      .header("Content-Disposition", "inline; filename="+fileName)
//    		      .body(bytes);	
//    }

	@RequestMapping(method = RequestMethod.GET, path = "/consignments/shipment")
	public List<ShipmentDetails> downloadShipmentData(@RequestParam("shipmentNumber") String shipmentNumber,
			@RequestParam("userId") Integer userId) {
		List<ShipmentDetails> senderData = d2zService.downloadShipmentData(shipmentNumber, userId);
		return senderData;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/consignments/shipmenttype")
	public List<ShipmentDetails> downloadShipmentDatabyType(@RequestParam("Data") List<String> Number,
			@RequestParam("userId") Integer userId,@RequestParam("Type") String Type) {
		
		List<ShipmentDetails> senderData = d2zService.downloadShipmentDatabyType(Number, userId,Type);
		return senderData;
	}

	
	
	@RequestMapping(method = RequestMethod.GET, path = "/consignments/shipmenttemplate")
	public List<ShipmentDetails> downloadShipmentDataTemplate(@RequestParam("shipmentNumber") String shipmentNumber,
			@RequestParam("userId") Integer userId) {
		List<ShipmentDetails> senderData = d2zService.downloadShipmentDataTemplate(shipmentNumber, userId);
		return senderData;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/consignments/shipmenttypetemplate")
	public List<ShipmentDetails> downloadShipmentDataTemplatebyType(@RequestParam("Data") List<String> Number,
			@RequestParam("userId") Integer userId,@RequestParam("Type") String Type) {
		
		List<ShipmentDetails> senderData = d2zService.downloadShipmentDataTemplatebyType(Number, userId,Type);
		return senderData;
	}
	@RequestMapping(method = RequestMethod.POST, path = "/ebay/completeSale")
	public UserMessage uploadShipmentToEbay(@RequestBody Ebay_ShipmentDetails shipmentDetails) {
		UserMessage userMsg = d2zService.uploadShipmentDetailsToEbay(shipmentDetails);
		return userMsg;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/client/dashbaord")
	public ClientDashbaord clientDahbaord(@RequestParam("userId") Integer userId) {
		ClientDashbaord clientDahbaord = d2zService.clientDahbaord(userId);
		return clientDahbaord;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/contactUs")
	public UserMessage contactUs(@RequestParam("email") String email, @RequestParam("message") String message,
			@RequestParam("name") String name, @RequestParam("subject") String subject) {
		UserMessage userMsg = d2zService.contactUs(email, message, name, subject);
		return userMsg;
	}

	@RequestMapping(method = RequestMethod.GET, path = "/freipost")
	public void triggerFreipost(@RequestBody String referenceNumbers) {
		d2zService.triggerFreipost(referenceNumbers);
	}

	@RequestMapping(method = RequestMethod.GET, path = "/FDM")
	public void triggerFDM() {
		d2zService.triggerFDM();
	}

	@RequestMapping(method = RequestMethod.GET, path = "/auPost")
	public void triggerAuPost() {
		d2zService.makeCalltoAusPost();
	}

	@RequestMapping(method = RequestMethod.GET, path = "/aupost/track-labels")
	public ResponseMessage auTrackingEvent() {
		return d2zService.auTrackingEvent();
	}

	@RequestMapping(method = RequestMethod.PUT, path = "/auPost")
	public void triggerAuPost(@RequestBody List<String> referenceNumbers) {
		d2zService.makeCalltoAusPost(referenceNumbers);
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/etowercall")
	public void triggeretower(@RequestBody List<String> referenceNumbers) {
		d2zService.makeCallToEtowerBasedonSupplierUI(referenceNumbers);
	}
	
	@RequestMapping(method = RequestMethod.PUT, path = "/consignments/shipmentarticleid/{shipmentNumber}")
	public ResponseMessage allocateShipmentbyArticleID(@RequestBody String articleid,@PathVariable String shipmentNumber)
			throws ReferenceNumberNotUniqueException {
		return d2zService.allocateShipmentArticleid(articleid.toString(), shipmentNumber);
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/currency")
	public void getcurrency() {
		d2zService.currencyRate();
		//superUserD2zService.triggerSC();
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/outStanding-returns")
	public List<Returns> returnsOutstanding(@RequestParam("fromDate") String fromDate, @RequestParam("toDate") String toDate, @RequestParam("userId") String userId) {
		System.out.println("Inside Outstanding Method");
		List<Returns> returnsInfo = d2zService.returnsOutstanding(fromDate, toDate, userId);
		return returnsInfo;
	}
	
}
