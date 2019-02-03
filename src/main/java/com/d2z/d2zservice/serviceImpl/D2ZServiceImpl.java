package com.d2z.d2zservice.serviceImpl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.d2z.d2zservice.dao.ID2ZDao;
import com.d2z.d2zservice.entity.SenderdataMaster;
import com.d2z.d2zservice.entity.Trackandtrace;
import com.d2z.d2zservice.entity.User;
import com.d2z.d2zservice.entity.UserService;
import com.d2z.d2zservice.excelWriter.ShipmentDetailsWriter;
import com.d2z.d2zservice.exception.InvalidUserException;
import com.d2z.d2zservice.exception.ReferenceNumberNotUniqueException;
import com.d2z.d2zservice.model.ClientDashbaord;
import com.d2z.d2zservice.model.CreateConsignmentRequest;
import com.d2z.d2zservice.model.DropDownModel;
import com.d2z.d2zservice.model.Ebay_Shipment;
import com.d2z.d2zservice.model.Ebay_ShipmentDetails;
import com.d2z.d2zservice.model.EditConsignmentRequest;
import com.d2z.d2zservice.model.ParcelStatus;
import com.d2z.d2zservice.model.ResponseMessage;
import com.d2z.d2zservice.model.SenderData;
import com.d2z.d2zservice.model.SenderDataResponse;
import com.d2z.d2zservice.model.ShipmentDetails;
import com.d2z.d2zservice.model.TrackParcel;
import com.d2z.d2zservice.model.TrackingDetails;
import com.d2z.d2zservice.model.TrackingEvents;
import com.d2z.d2zservice.model.UserDetails;
import com.d2z.d2zservice.model.UserMessage;
import com.d2z.d2zservice.proxy.EbayProxy;
import com.d2z.d2zservice.repository.UserRepository;
import com.d2z.d2zservice.service.ID2ZService;
import com.d2z.d2zservice.validation.D2ZValidator;
import com.ebay.soap.eBLBaseComponents.CompleteSaleResponseType;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRSaver;
import uk.org.okapibarcode.backend.DataMatrix;
import uk.org.okapibarcode.backend.DataMatrix.ForceMode;
import uk.org.okapibarcode.backend.OkapiException;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.backend.Symbol.DataType;
import uk.org.okapibarcode.output.Java2DRenderer;

@Service
public class D2ZServiceImpl implements ID2ZService{
	
	@Autowired
    private ID2ZDao d2zDao;

	@Autowired
    private D2ZValidator d2zValidator;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	ShipmentDetailsWriter shipmentWriter;

	@Autowired
	private EbayProxy proxy;
	
	@Override
	public List<SenderDataResponse> exportParcel(List<SenderData> orderDetailList) throws ReferenceNumberNotUniqueException{
		d2zValidator.isReferenceNumberUnique(orderDetailList);
		d2zValidator.isServiceValidUI(orderDetailList);
		d2zValidator.isPostCodeValid(orderDetailList);
		String senderFileID = d2zDao.exportParcel(orderDetailList);
		List<String> insertedOrder = d2zDao.fetchBySenderFileID(senderFileID);
		List<SenderDataResponse> senderDataResponseList = new ArrayList<SenderDataResponse>();
		SenderDataResponse senderDataResponse = null;
		Iterator itr = insertedOrder.iterator();
		 while(itr.hasNext()) {   
			 Object[] obj = (Object[]) itr.next();
			 senderDataResponse = new SenderDataResponse();
			 senderDataResponse.setReferenceNumber(obj[0].toString());
			 senderDataResponse.setBarcodeLabelNumber("]d2".concat(obj[1].toString().replaceAll("\\[|\\]", "")));
			 senderDataResponseList.add(senderDataResponse);
       }
		
		return senderDataResponseList;
	}
	
	@Override
	public UserMessage consignmentDelete(String refrenceNumlist) {
		String fileUploadData= d2zDao.consignmentDelete(refrenceNumlist);
		UserMessage userMsg = new UserMessage();
		userMsg.setMessage(fileUploadData);
		return userMsg;
	}

	@Override
	public List<DropDownModel> fileList(Integer userId) {
		List<String> listOfFileNames= d2zDao.fileList(userId);
		List<DropDownModel> dropDownList= new ArrayList<DropDownModel>();
		for(String fileName:listOfFileNames) {
			DropDownModel dropDownVaL = new DropDownModel();
			dropDownVaL.setName(fileName);
			dropDownVaL.setValue(fileName);
			dropDownList.add(dropDownVaL);
		}
		return dropDownList;
	}
	
	@Override
	public List<DropDownModel> labelFileList(Integer userId) {
		List<String> listOfFileNames= d2zDao.labelFileList(userId);
		List<DropDownModel> dropDownList= new ArrayList<DropDownModel>();
		for(String fileName:listOfFileNames) {
			DropDownModel dropDownVaL = new DropDownModel();
			dropDownVaL.setName(fileName);
			dropDownVaL.setValue(fileName);
			dropDownList.add(dropDownVaL);
		}
		return dropDownList;
	}
	
	@Override
	public List<TrackingDetails> trackingDetails(String fileName) {
		List<TrackingDetails> trackingDetailsList = new ArrayList<TrackingDetails>();
		TrackingDetails trackingDetails = null;
		List<String> trackingService = d2zDao.trackingDetails(fileName);
		Iterator itr = trackingService.iterator();
		 while(itr.hasNext()) {   
			 Object[] obj = (Object[]) itr.next();
			 trackingDetails = new TrackingDetails();
			 trackingDetails.setRefrenceNumber(obj[0].toString());
			 trackingDetails.setConsigneeName(obj[1].toString());
			 trackingDetails.setBarCodeLabelNumber(obj[2].toString());
			 trackingDetailsList.add(trackingDetails);
        }
		return trackingDetailsList;
	}

	@Override
	public List<SenderdataMaster> consignmentFileData(String fileName) {
		List<SenderdataMaster> fileData= d2zDao.consignmentFileData(fileName);
		return fileData;
	}

	@Override
	public byte[] generateLabel(List<SenderData> senderData) {
		
		for(SenderData data : senderData){
			data.setDatamatrixImage(generateDataMatrix(data.getDatamatrix()));
		}
		JRBeanCollectionDataSource beanColDataSource =
		         new JRBeanCollectionDataSource(senderData);
		 Map<String,Object> parameters = new HashMap<>();
		 byte[] bytes = null;
		 //Blob blob = null;
		    JasperReport jasperReport = null;
		    try (ByteArrayOutputStream byteArray = new ByteArrayOutputStream()) {
		    	jasperReport  = JasperCompileManager.compileReport(getClass().getResource("/eparcelLabel.jrxml").openStream());
		        JRSaver.saveObject(jasperReport, "label.jasper");
		        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanColDataSource);
		      // return the PDF in bytes
		      bytes = JasperExportManager.exportReportToPdf(jasperPrint);
			// blob = new javax.sql.rowset.serial.SerialBlob(bytes);
		    }
		    catch (JRException | IOException  e) {
		      e.printStackTrace();
		    }
		    return bytes;
	}

	private BufferedImage generateDataMatrix(String datamatrixInput) {
		BufferedImage datamatrixImage = null;
		 try{
	            // Set up the DataMatrix object
	            DataMatrix dataMatrix = new DataMatrix();
	            // We need a GS1 DataMatrix barcode.
	            dataMatrix.setDataType(DataType.GS1); 
	            // 0 means size will be set automatically according to amount of data (smallest possible).
	            dataMatrix.setPreferredSize(0); 
	            // Don't want no funky rectangle shapes, if we can avoid it.
	            dataMatrix.setForceMode(ForceMode.SQUARE); 
	            dataMatrix.setContent(datamatrixInput);
	            datamatrixImage= getMagnifiedBarcode(dataMatrix);
	            //return getBase64FromByteArrayOutputStream(getMagnifiedBarcode(dataMatrix, MAGNIFICATION));
	        } catch(OkapiException oe){
	        	oe.printStackTrace();
	        }
         return datamatrixImage;

		}

	private  BufferedImage getMagnifiedBarcode(Symbol symbol){
		final int MAGNIFICATION = 10;
		final int BORDER_SIZE = 0 * MAGNIFICATION;
        // Make DataMatrix object into bitmap
        BufferedImage image = new BufferedImage((symbol.getWidth() * MAGNIFICATION) + (2 * BORDER_SIZE),
                (symbol.getHeight() * MAGNIFICATION) + (2 * BORDER_SIZE),BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0,  (symbol.getWidth() * MAGNIFICATION) + (2 * BORDER_SIZE),
                (symbol.getHeight() * MAGNIFICATION) + (2 * BORDER_SIZE));
        Java2DRenderer renderer = new Java2DRenderer(g2d, 10, Color.WHITE, Color.BLACK);
        renderer.render(symbol);
        
        return image;
    }

	@Override
	public  byte[] trackingLabel(String refBarNum) {
		SenderData trackingLabel = new SenderData();
		List<SenderData> trackingLabelList = new ArrayList<SenderData>();
		String trackingLabelData = d2zDao.trackingLabel(refBarNum);
		String[] trackingArray = trackingLabelData.split(",");
		trackingLabel.setReferenceNumber(trackingArray[0]);
		trackingLabel.setConsigneeName(trackingArray[1]);
		trackingLabel.setConsigneeAddr1(trackingArray[2]);
		trackingLabel.setConsigneeSuburb(trackingArray[3]);
		trackingLabel.setConsigneeState(trackingArray[4]);
		trackingLabel.setConsigneePostcode(trackingArray[5]);
		trackingLabel.setConsigneePhone(trackingArray[6]);
		trackingLabel.setWeight(trackingArray[7]);
		trackingLabel.setShipperName(trackingArray[8]);
		trackingLabel.setShipperAddr1(trackingArray[9]);
		trackingLabel.setShipperCity(trackingArray[10]);
		trackingLabel.setShipperState(trackingArray[11]);
		trackingLabel.setShipperCountry(trackingArray[12]);
		trackingLabel.setShipperPostcode(trackingArray[13]);
		trackingLabel.setBarcodeLabelNumber(trackingArray[14]);
		trackingLabel.setDatamatrix(trackingArray[15]);
		trackingLabel.setInjectionState(trackingArray[16]);
		trackingLabel.setDatamatrixImage(generateDataMatrix(trackingLabel.getDatamatrix()));
		trackingLabelList.add(trackingLabel);
		JRBeanCollectionDataSource beanColDataSource =
		         new JRBeanCollectionDataSource(trackingLabelList);
		 Map<String,Object> parameters = new HashMap<>();
		 byte[] bytes = null;
		 //Blob blob = null;
		    JasperReport jasperReport = null;
		    try (ByteArrayOutputStream byteArray = new ByteArrayOutputStream()) {
		    	jasperReport  = JasperCompileManager.compileReport(getClass().getResource("/eparcelLabel.jrxml").openStream());
		        JRSaver.saveObject(jasperReport, "label.jasper");
		        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanColDataSource);
		      // return the PDF in bytes
		      bytes = JasperExportManager.exportReportToPdf(jasperPrint);
			// blob = new javax.sql.rowset.serial.SerialBlob(bytes);
		    }
		    catch (JRException | IOException  e) {
		      e.printStackTrace();
		    }
		    return bytes;
	}

	@Override
	public UserMessage manifestCreation(String manifestNumber, String refrenceNumber) {
		String fileUploadData= d2zDao.manifestCreation(manifestNumber, refrenceNumber);
		UserMessage userMsg = new UserMessage();
		userMsg.setMessage(fileUploadData);
		return userMsg;
	}
	
/*	public List<TrackParcel> trackParcel(List<String> referenceNumbers,List<String> articleIDs) {
		List<TrackParcel> trackParcelList  = new ArrayList<TrackParcel>();
		if(!referenceNumbers.isEmpty()) {
			trackParcelList = trackParcelByRefNbr(referenceNumbers);
		}
		else if(!articleIDs.isEmpty()) {
			trackParcelList = trackParcelByArticleID(articleIDs);
		}
		return trackParcelList;
	}*/

	public List<TrackParcel> trackParcelByArticleID(List<String> articleIDs ) {
		List<TrackParcel> trackParcelList  = new ArrayList<TrackParcel>();

		for(String articleID :articleIDs ) {
			List<Trackandtrace> trackAndTraceList= d2zDao.trackParcelByArticleID(articleID);
			TrackParcel trackParcel = new TrackParcel();
			//List<TrackEventDetails> trackEventDetails = new ArrayList<TrackEventDetails>();
			List<TrackingEvents> trackingEventList = new ArrayList<TrackingEvents>();
			for (Trackandtrace daoObj : trackAndTraceList) {
				TrackingEvents trackingEvents = new TrackingEvents();
				trackParcel.setReferenceNumber(daoObj.getReference_number());
				trackParcel.setBarcodelabelNumber(daoObj.getArticleID());
				trackingEvents.setEventDetails(daoObj.getTrackEventDetails());
				trackingEvents.setTrackEventDateOccured(daoObj.getTrackEventDateOccured());
				trackingEventList.add(trackingEvents);
			/*	switch(daoObj.getTrackEventDetails().toUpperCase()) {
					
					case "CONSIGNMENT CREATED":
						trackParcel.setConsignmentCreated(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "SHIPMENT ALLOCATED":
						trackParcel.setShipmentCreated(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "HELD BY CUSTOMS":
						trackParcel.setHeldByCustoms(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "CLEARED CUSTOMS":
						trackParcel.setClearedCustoms(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "RECEIVED":
						trackParcel.setReceived(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "PROCESSED BY FACILITY":
						trackParcel.setProcessedByFacility(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "IN TRANSIT":
						trackParcel.setInTransit(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "DELIVERED":
						trackParcel.setDelivered(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
				}
				*/
				/*TrackEventDetails trackEventDetail =  new TrackEventDetails();
				trackEventDetail.setTrackEventDateOccured(daoObj.getTrackEventDateOccured());
				trackEventDetail.setTrackEventDetails(daoObj.getTrackEventDetails());
				
				trackEventDetails.add(trackEventDetail);
				trackParcel.setTrackEventDetails(trackEventDetails);*/

			}
			trackParcel.setTrackingEvents(trackingEventList);

			trackParcelList.add(trackParcel);
		}
		return trackParcelList;
	}

	public List<TrackParcel> trackParcelByRefNbr(List<String> referenceNumbers) {
		List<TrackParcel> trackParcelList  = new ArrayList<TrackParcel>();
		
		for(String refNbr :referenceNumbers ) {
			List<Trackandtrace> trackAndTraceList= d2zDao.trackParcel(refNbr);
			//List<TrackEventDetails> trackEventDetails = new ArrayList<TrackEventDetails>();
			TrackParcel trackParcel = new TrackParcel();
			List<TrackingEvents> trackingEventList = new ArrayList<TrackingEvents>();
			for (Trackandtrace daoObj : trackAndTraceList) {
				System.out.println(daoObj);
				TrackingEvents trackingEvents = new TrackingEvents();
				trackParcel.setReferenceNumber(refNbr);
				trackParcel.setBarcodelabelNumber(daoObj.getArticleID());
				trackingEvents.setEventDetails(daoObj.getTrackEventDetails());
				trackingEvents.setTrackEventDateOccured(daoObj.getTrackEventDateOccured());
				trackingEventList.add(trackingEvents);
				/*switch(daoObj.getTrackEventDetails().toUpperCase()) {
					
					case "CONSIGNMENT CREATED":
						trackParcel.setConsignmentCreated(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "SHIPMENT ALLOCATED":
						trackParcel.setShipmentCreated(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "HELD BY CUSTOMS":
						trackParcel.setHeldByCustoms(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "CLEARED CUSTOMS":
						trackParcel.setClearedCustoms(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "RECEIVED AND CLEAR":
						trackParcel.setReceived(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "PROCESSED BY FACILITY":
						trackParcel.setProcessedByFacility(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "IN TRANSIT":
						trackParcel.setInTransit(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
					case "DELIVERED":
						trackParcel.setDelivered(String.valueOf(daoObj.getTrackEventDateOccured()));
						break;
				}*/
			/*	TrackEventDetails trackEventDetail =  new TrackEventDetails();
				trackEventDetail.setTrackEventDateOccured(daoObj.getTrackEventDateOccured());
				trackEventDetail.setTrackEventDetails(daoObj.getTrackEventDetails());
				trackEventDetails.add(trackEventDetail);
				trackParcel.setTrackEventDetails(trackEventDetails);*/
				
			}
			trackParcel.setTrackingEvents(trackingEventList);
			trackParcelList.add(trackParcel);
		}
		return trackParcelList;
	}

	@Override
	public List<SenderDataResponse> createConsignments(CreateConsignmentRequest orderDetail) throws ReferenceNumberNotUniqueException {
		Integer userId = userRepository.fetchUserIdbyUserName(orderDetail.getUserName());
		if(userId == null) {
			throw new InvalidUserException("User does not exist",orderDetail.getUserName());
		}
		System.out.println(userId);
		d2zValidator.isReferenceNumberUnique(orderDetail.getConsignmentData());
		d2zValidator.isServiceValid(orderDetail);
		d2zValidator.isPostCodeValid(orderDetail.getConsignmentData());
		String senderFileID = d2zDao.createConsignments(orderDetail.getConsignmentData(),userId);
		List<String> insertedOrder = d2zDao.fetchBySenderFileID(senderFileID);
		List<SenderDataResponse> senderDataResponseList = new ArrayList<SenderDataResponse>();
		SenderDataResponse senderDataResponse = null;
		Iterator itr = insertedOrder.iterator();
		 while(itr.hasNext()) {   
			 Object[] obj = (Object[]) itr.next();
			 senderDataResponse = new SenderDataResponse();
			 senderDataResponse.setReferenceNumber(obj[0].toString());
			 String barcode = obj[1].toString();
			 //String formattedBarcode = barcode.substring(0,barcode.length()-6).concat("120000");
			 senderDataResponse.setBarcodeLabelNumber("]d2".concat(barcode.replaceAll("\\[|\\]", "")));
			 senderDataResponseList.add(senderDataResponse);
       }
		
		return senderDataResponseList;
	}

	@Override
	public ResponseMessage editConsignments(List<EditConsignmentRequest> requestList) {
		return d2zDao.editConsignments(requestList);
	}

	@Override
	public ResponseMessage allocateShipment(String referenceNumbers, String shipmentNumber) {
		ResponseMessage userMsg = new ResponseMessage();

		String[] refNbrs = referenceNumbers.split(",");
		List<String> incorrectRefNbr = d2zDao.findRefNbrByShipmentNbr(refNbrs);
		
		if(!incorrectRefNbr.isEmpty()) {
			userMsg.setResponseMessage("Shipment Number already allocated");
			userMsg.setMessageDetail(incorrectRefNbr);
			return userMsg;
		}
		String msg = d2zDao.allocateShipment(referenceNumbers,shipmentNumber);
		userMsg.setResponseMessage(msg);
		return userMsg;
	}

	@Override
	public UserMessage addUser(UserDetails userData) {
		UserMessage userMsg = new UserMessage();
		User existingUser = userRepository.fetchUserbyCompanyName(userData.getCompanyName(), userData.getRole_Id());
		if(existingUser == null) {
			List<String> existingUserNames = userRepository.fetchAllUserName();
			if(existingUserNames.contains(userData.getUserName())){
				userMsg.setMessage("UserName already exist");
				userMsg.setUserName(userData.getUserName());
				return userMsg;
			}
 			User savedUser = d2zDao.addUser(userData);
			if(savedUser.getUser_Id()!= 0) {
				List<UserService> savedUserService = d2zDao.addUserService(savedUser,userData.getServiceType());
				if(savedUserService.size()!=0) {
					userMsg.setMessage("User Added Successfully");
					userMsg.setUserName(userData.getUserName());
				}
			}
			else {
				userMsg.setMessage("Unable to Add User");
				userMsg.setUserName(userData.getUserName());
			}
		}else {
			userMsg.setMessage("Company Name already exist");
			userMsg.setCompanyName(userData.getCompanyName());
		}
		
		return userMsg;
	}

	@Override
	public UserMessage updateUser(UserDetails userDetails) {
		UserMessage userMsg = new UserMessage();
		User existingUser = userRepository.fetchUserbyCompanyName(userDetails.getCompanyName(), userDetails.getRole_Id());
		if(existingUser == null) {
			userMsg.setMessage("Company Name does not exist");
			userMsg.setCompanyName(userDetails.getCompanyName());
			return userMsg;
		}
		else {
			 if(!existingUser.getUser_Name().equals(userDetails.getUserName())) {
				List<String> existingUserNames = userRepository.fetchAllUserName();
				if(existingUserNames.contains(userDetails.getUserName())) {
					userMsg.setMessage("UserName already exist");
					userMsg.setUserName(userDetails.getUserName());
					return userMsg;
				}
				existingUser.setUser_Name(userDetails.getUserName());
			 }
				existingUser.setAddress(userDetails.getAddress());
				existingUser.setState(userDetails.getState());
				existingUser.setSuburb(userDetails.getSuburb());
				existingUser.setPostcode(userDetails.getPostCode());
				existingUser.setCountry(userDetails.getCountry());
				existingUser.setEmailAddress(userDetails.getEmailAddress());
				existingUser.setUser_Password(userDetails.getPassword());
				existingUser.setEBayToken(userDetails.geteBayToken());
				existingUser.setModifiedTimestamp(Timestamp.valueOf(LocalDateTime.now()));
				User updatedUser = d2zDao.updateUser(existingUser);
				d2zDao.updateUserService(updatedUser,userDetails);
				userMsg.setMessage("Updated Successfully");
		}
		return userMsg;
	}

	@Override
	public UserMessage deleteUser(String companyName, String roleId) {
		UserMessage userMsg = new UserMessage();
		userMsg.setCompanyName(companyName);
		String msg = d2zDao.deleteUser(companyName, roleId);
		userMsg.setMessage(msg);
		return userMsg;
	}


	public List<SenderdataMaster> fetchManifestData(String fileName) {
		List<SenderdataMaster> fileData= d2zDao.fetchManifestData(fileName);
		return fileData;
	}

	@Override
	public UserDetails login(String userName, String passWord) {
		User userData = d2zDao.login(userName, passWord);
		List<String> serviceType = null;
		UserDetails userDetails = new UserDetails();
		userDetails.setAddress(userData.getAddress());
		userDetails.setCompanyName(userData.getCompanyName());
		userDetails.setContactName(userData.getName());
		userDetails.setContactPhoneNumber(userData.getPhoneNumber());
		userDetails.setCountry(userData.getCountry());
		userDetails.setEmailAddress(userData.getEmailAddress());
		userDetails.setPassword(userData.getUser_Password());
		userDetails.setPostCode(userData.getPostcode());
		userDetails.setState(userData.getState());
		userDetails.setSuburb(userData.getSuburb());
		userDetails.setUserName(userData.getUser_Name());
		userDetails.setRole_Id(userData.getRole_Id());
		userDetails.setUser_id(userData.getUser_Id());
		Set<UserService> userServiceList = userData.getUserService();
		if(userServiceList.size() > 0) {
			serviceType = userServiceList.stream().map(obj ->{
				return obj.getServiceType();}).collect(Collectors.toList());
		}
		userDetails.setServiceType(serviceType);
		return userDetails;
	}

	@Override
	//public byte[] downloadShipmentData(String shipmentNumber) {
	public List<ShipmentDetails>  downloadShipmentData(String shipmentNumber) {
		List<SenderdataMaster> senderDataList  = d2zDao.fetchShipmentData(shipmentNumber);
		System.out.println(senderDataList.size()+" records");
		List<ShipmentDetails> shipmentDetails = new ArrayList<ShipmentDetails>();
		for(SenderdataMaster senderData : senderDataList) {
			ShipmentDetails shipmentData = new ShipmentDetails();
			shipmentData.setReferenceNumber(senderData.getReference_number());
			shipmentData.setCon_no(senderData.getBarcodelabelNumber().substring(19,30));
			shipmentData.setConsigneeName(senderData.getConsignee_name());
			shipmentData.setConsigneeAddress(senderData.getConsignee_addr1());
			shipmentData.setWeight(senderData.getWeight());
			shipmentData.setConsigneePhone(senderData.getConsignee_Phone());
			shipmentData.setConsigneeSuburb(senderData.getConsignee_Suburb());
			shipmentData.setConsigneeState(senderData.getConsignee_State());
			shipmentData.setConsigneePostcode(senderData.getConsignee_Postcode());
			shipmentData.setDestination("AUSTRALIA");
			shipmentData.setQuantity(senderData.getShippedQuantity());
			shipmentData.setCommodity(senderData.getProduct_Description());
			shipmentData.setValue(senderData.getValue());
			shipmentData.setShipperName(senderData.getShipper_Name());
			shipmentData.setShipperAddress(senderData.getShipper_Addr1());
			shipmentData.setShipperCity(senderData.getShipper_City());
			shipmentData.setShipperState(senderData.getShipper_State());
			shipmentData.setShipperPostcode(senderData.getShipper_Postcode());
			shipmentData.setShipperCountry(senderData.getShipper_Country());
			shipmentData.setShipperContact(senderData.getAirwayBill());
			shipmentDetails.add(shipmentData);
		}
//		byte[] bytes = shipmentWriter.generateShipmentxls(shipmentDetails);
//		return bytes;
		return shipmentDetails;
	}


	@Override
	public List<ParcelStatus> getStatusByRefNbr(List<String> referenceNumbers) {
		List<ParcelStatus> trackParcelList  = new ArrayList<ParcelStatus>();

		for(String referenceNumber :referenceNumbers) {
			Trackandtrace trackAndTrace= d2zDao.getLatestStatusByReferenceNumber(referenceNumber);
			if(trackAndTrace!=null) {
			ParcelStatus trackParcel = new ParcelStatus();
			trackParcel.setReferenceNumber(trackAndTrace.getReference_number());
			trackParcel.setArticleID(trackAndTrace.getArticleID());
			trackParcel.setTrackEventDateOccured(String.valueOf(trackAndTrace.getTrackEventDateOccured()));
			trackParcel.setTrackEventDetails(trackAndTrace.getTrackEventDetails());
			trackParcelList.add(trackParcel);
			}
		}
		
		return trackParcelList;
	}

	@Override
	public List<ParcelStatus> getStatusByArticleID(List<String> articleIDs) {

		List<ParcelStatus> trackParcelList  = new ArrayList<ParcelStatus>();

		for(String articleID :articleIDs ) {
			Trackandtrace trackAndTrace= d2zDao.getLatestStatusByArticleID(articleID);
			if(trackAndTrace!=null) {
			ParcelStatus trackParcel = new ParcelStatus();
			trackParcel.setReferenceNumber(trackAndTrace.getReference_number());
			trackParcel.setArticleID(trackAndTrace.getArticleID());
			trackParcel.setTrackEventDateOccured(String.valueOf(trackAndTrace.getTrackEventDateOccured()));
			trackParcel.setTrackEventDetails(trackAndTrace.getTrackEventDetails());
			trackParcelList.add(trackParcel);
			}
		}
		
		return trackParcelList;
	
	}

	@Override
	public UserMessage uploadShipmentDetailsToEbay(Ebay_ShipmentDetails shipmentDetails) {
		UserMessage userMsg = new UserMessage();
		int successCount=0;
		for(Ebay_Shipment shipment : shipmentDetails.getShipment()) {
		CompleteSaleResponseType response = proxy.makeCalltoEbay_CompleteSale(shipment,shipmentDetails.getClientEbayToken());	
		if(response!=null && "SUCCESS".equalsIgnoreCase(response.getAck().toString())) {
			successCount++;
		}
		if(successCount == shipmentDetails.getShipment().size()) {
			userMsg.setMessage("Data sucessfully uploaded to EBay Server");
		}
		else if(successCount==0) {
			userMsg.setMessage("Failed to upload data to EBay Server");
		}
		else {
			userMsg.setMessage("Data Partially uploaded to Ebay Server");
		}
		}
		return userMsg;
	}

	@Override
	public ClientDashbaord clientDahbaord(Integer userId) {
		ClientDashbaord clientDashbaord = d2zDao.clientDahbaord(userId);
		return clientDashbaord;
	}
}
