package com.d2z.d2zservice.serviceImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.d2z.d2zservice.dao.ID2ZBrokerDao;
import com.d2z.d2zservice.entity.SenderdataMaster;
import com.d2z.d2zservice.entity.User;
import com.d2z.d2zservice.entity.UserService;
import com.d2z.d2zservice.model.DirectInjectionDetails;
import com.d2z.d2zservice.model.DropDownModel;
import com.d2z.d2zservice.model.UserDetails;
import com.d2z.d2zservice.service.IBrokerD2ZService;

@Service
public class BrokerD2ZServiceImpl implements IBrokerD2ZService{
	
	@Autowired
    private ID2ZBrokerDao d2zDao;
	
	@Override
	public List<DropDownModel> companyDetails(String brokerId) {
		List<String> listOfCompany = d2zDao.companyDetails(brokerId);
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
	public UserDetails fetchUserDetails(String companyName, String roleId) {
		User user = d2zDao.fetchUserDetails(companyName, roleId);
		UserDetails userDetails = new UserDetails();
		if(user != null) {
			userDetails.setAddress(user.getAddress());
			userDetails.setCompanyName(user.getCompanyName());
			userDetails.setContactName(user.getName());
			userDetails.setContactPhoneNumber(user.getPhoneNumber());
			userDetails.setCountry(user.getCountry());
			userDetails.setEmailAddress(user.getEmailAddress());
			userDetails.setPassword(user.getUser_Password());
			userDetails.setPostCode(user.getPostcode());
			userDetails.setState(user.getState());
			userDetails.setSuburb(user.getSuburb());
			userDetails.setUserName(user.getUser_Name());
			userDetails.seteBayToken(user.getEBayToken());
			Set<UserService> userServiceList = user.getUserService();
			List<String> serviceType = userServiceList.stream().map(obj -> { 
				 System.out.println("filter: " + obj);
				return obj.getServiceType();}).collect(Collectors.toList());
			userDetails.setServiceType(serviceType);
		}
		return userDetails;
	}

	@Override
	public List<DropDownModel> getManifestList() {
		List<String> listOfManifestId = d2zDao.getManifestList();
		List<DropDownModel> manifestDropDownList= new ArrayList<DropDownModel>();
		for(String manifestId:listOfManifestId) {
			if(manifestId != null) {
				DropDownModel dropDownVaL = new DropDownModel();
				dropDownVaL.setName(manifestId);
				dropDownVaL.setValue(manifestId);
				manifestDropDownList.add(dropDownVaL);
			}
		}
		return manifestDropDownList;
	}

	@Override
	public List<SenderdataMaster> consignmentDetails(String manifestNumber) {
		List<SenderdataMaster> consignmentDetails = d2zDao.consignmentDetails(manifestNumber);
		return consignmentDetails;
	}

	@Override
	public List<DropDownModel> fetchShipmentList() {
		List<String> listOfShipment = d2zDao.fetchShipmentList();
		List<DropDownModel> shipmentDropDownList= new ArrayList<DropDownModel>();
		for(String manifestId:listOfShipment) {
			if(manifestId != null) {
				DropDownModel dropDownVaL = new DropDownModel();
				dropDownVaL.setName(manifestId);
				dropDownVaL.setValue(manifestId);
				shipmentDropDownList.add(dropDownVaL);
			}
		}
		return shipmentDropDownList;
	}

	@Override
	public List<DirectInjectionDetails> directInjection(String companyName) {
		List<DirectInjectionDetails> directInjectionDetails = new ArrayList<DirectInjectionDetails>();
		DirectInjectionDetails directInjection = null;
		List<String> trackingService = d2zDao.directInjection(companyName);
		Iterator itr = trackingService.iterator();
		 while(itr.hasNext()) {   
			 Object[] obj = (Object[]) itr.next();
			 directInjection = new DirectInjectionDetails();
			 directInjection.setReferenceNumber(obj[0].toString());
			 directInjection.setArticleId(obj[1].toString());
			 directInjection.setConsigneeName(obj[2].toString());
			 directInjection.setPostCode(obj[3].toString());
			 directInjection.setWeight(obj[4].toString());
			 directInjection.setShipperName(obj[5].toString());
			 directInjectionDetails.add(directInjection);
        }
		return directInjectionDetails;
	}

	@Override
	public List<DropDownModel> fetchApiShipmentList() {
		List<String> listOfApiShipment = d2zDao.fetchApiShipmentList();
		List<DropDownModel> apiShipmentDropDownList= new ArrayList<DropDownModel>();
		for(String shipmentId:listOfApiShipment) {
			if(shipmentId != null) {
				DropDownModel dropDownVaL = new DropDownModel();
				dropDownVaL.setName(shipmentId);
				dropDownVaL.setValue(shipmentId);
				apiShipmentDropDownList.add(dropDownVaL);
			}
		}
		return apiShipmentDropDownList;
	}

	@Override
	public List<SenderdataMaster> downloadShipmentData(String shipmentNumber) {
		// TODO Auto-generated method stub
		return d2zDao.fetchShipmentData(shipmentNumber);
	}

	
}
