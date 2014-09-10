package org.speedd.fraud;

import org.junit.Test;
import org.speedd.data.Event;
import org.speedd.data.impl.SpeeddEventFactory;

import java.util.Map;

import static org.junit.Assert.*;

public class FraudAggregatedReadingCsv2EventTest {
	private static final FraudAggregatedReadingCsv2Event parser = new FraudAggregatedReadingCsv2Event(SpeeddEventFactory.getInstance());

	@Test
	public void testCsv2Event() throws Exception {
		String eventCsv = "1423150200000,TXN_ID,1,250,567745453,201702,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,1532322,201801,17,18,0";
		
		Event event = parser.csv2event(eventCsv);
		
		assertNotNull(event);

		assertEquals(Constants.TRANSACTION, event.getEventName());

		Map<String, Object> attrs = event.getAttributes();
        assertEquals(1423150200000L, attrs.get("timestamp"));
        assertEquals("TXN_ID", attrs.get("transaction_id"));
        assertEquals(true, attrs.get("is_cnp"));
        assertEquals(250.0, attrs.get("amount_eur"));
        assertEquals("567745453", attrs.get("card_pan"));
        assertEquals("201702", attrs.get("card_exp"));
        assertEquals(1, attrs.get("card_country"));
        assertEquals(2, attrs.get("card_family"));
        assertEquals(3, attrs.get("card_type"));
        assertEquals(4, attrs.get("card_tech"));
        assertEquals(5, attrs.get("acquirer_country"));
        assertEquals(6, attrs.get("merchant_mcc"));
        assertEquals(7L, attrs.get("terminal_brand"));
        assertEquals(8L, attrs.get("terminal_id"));
        assertEquals(9, attrs.get("terminal_type"));
        assertEquals(10, attrs.get("terminal_emv"));
        assertEquals(11, attrs.get("transaction_response"));
        assertEquals(12, attrs.get("card_auth"));
        assertEquals(13, attrs.get("terminal_auth"));
        assertEquals(14, attrs.get("client_auth"));
        assertEquals(15, attrs.get("card_brand"));
        assertEquals(16, attrs.get("cvv_validation"));
        assertEquals("1532322", attrs.get("tmp_card"));
        assertEquals("201801", attrs.get("tmp_card_exp_date"));
        assertEquals(17, attrs.get("transaction_type"));
        assertEquals(18, attrs.get("auth_type"));
        assertEquals(false, attrs.get("is_fraud"));
	}
}
