package com.d2z.d2zservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.d2z.d2zservice.entity.NonD2ZData;

public interface NonD2ZDataRepository extends CrudRepository<NonD2ZData, Long>{
	
	@Query(value="Select articleId from NonD2ZData") 
	List<String> fetchAllArticleId();
	
	@Procedure(name = "RatesND")
	void nonD2zRates(@Param("ArticleId") String ArticleId);
	
	@Query(value="Select c from NonD2ZData c where c.articleId = :articleNo") 
	NonD2ZData reconcileNonD2zData(@Param("articleNo") String articleNo);

	@Procedure(name = "reconcileratesND")
	void reconcileratesND(@Param("Reference_number") String Reference_number);
	
	@Query(value="Select distinct c.brokerName,c.shipmentNumber from NonD2ZData c where c.invoiced is null")
	List<NonD2ZData> brokerNonD2zShipment();

	@Query(nativeQuery = true, value="SELECT DISTINCT S.articleid  AS TrackingNumber, S.reference_number AS reference, "
			+ "S.postcode AS postcode, S.weight AS Weight, B.rate AS postage, S.fuelsurcharge AS Fuelsurcharge, "
			+ "S.brokerrate AS total FROM NonD2ZData S INNER JOIN POSTCODEZONES P ON P.postcode = S.postcode AND "
			+ "P.suburb = S.suburb INNER JOIN BROKERRATES B ON S.ShipmentNumber in (:airwayBill) AND "
			+ "B.brokerusername in (:broker) AND ( S.weight BETWEEN Cast(B.minweight AS DECIMAL(18, 4)) "
			+ "AND Cast( B.maxweight AS DECIMAL(18, 4)) ) AND S.servicetype = B.servicetype AND S.postcode = P.postcode "
			+ "AND B.zoneid = P.zone")
	List<String> downloadNonD2zInvoice(@Param("broker") List<String> broker, @Param("airwayBill")  List<String> airwayBill);

	@Procedure(name = "InvoiceUpdateND")
	void approveNdInvoiced(@Param("Indicator") String Indicator, @Param("Airwaybill") String Airwaybill);
	
	@Query(value="Select distinct c.brokerName,c.shipmentNumber from NonD2ZData c where c.invoiced = 'Y' and c.billed is null")
	List<NonD2ZData> brokerNdInvoiced();

}
