package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.StackLocations;
import au.gov.nla.pickslip.TestUtils;
import au.gov.nla.pickslip.domain.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class PickslipQueuesTests {

    @Spy FolioService folioService;

    @Test
    void updatePickslipQueues() throws IOException {

        // Mockito.when..
        Mockito.doReturn(TestUtils.loadJson("folioServicePoints.json"))
                .when(folioService).folioApiGetServicePoints();
        Mockito.doReturn(TestUtils.loadJson("folioLocations.json"))
                .when(folioService).folioApiGetFolioLocations();
        Mockito.doReturn(TestUtils.loadJson("pickslipsForServicePointMitchell.json"))
                .when(folioService).folioApiGetPickslipsForServicePoint(
                        ArgumentMatchers.eq("343d85b5-654b-4331-aae3-67c434d57316"),
                        ArgumentMatchers.anyInt());
        Mockito.doReturn(TestUtils.loadJson("folioRequests.json"))
                .when(folioService).folioApiGetRequests();

        // setup..
        List<FolioServicePoint> servicePoints = folioService.getFolioServicePoints();
        List<FolioLocation> locations = folioService.getFolioLocations();
        List<FolioPickslip> pickslips = folioService.getPickslipsForServicePoint("343d85b5-654b-4331-aae3-67c434d57316");
        List<FolioRequest> requests = folioService.getFolioRequests();

        FolioServicePoint mitchellServicePoint = servicePoints.get(0);
        var pickslipsForMitchell = Map.of(mitchellServicePoint, pickslips);

        StackLocations stackLocations = new StackLocations();
        stackLocations.setStacks(Arrays.asList(new StackLocations.Location("MITCHELL-SP", "Mitchell"),
                new StackLocations.Location("MRR-SP", "Main Reading Room")));

        // test..
        PickslipQueues pickslipQueues = new PickslipQueues();
        pickslipQueues.update(
                stackLocations, requests, servicePoints, locations, pickslipsForMitchell);

        // expected..
        List<PickslipQueues.Pickslip> mrrPickslips = pickslipQueues.getPickslipsForStack("MITCHELL-SP");

        Assertions.assertAll("Pickslip for Mitchell-SP",
                () -> Assertions.assertEquals(1, mrrPickslips.size(), "Should be one item"),
                () -> Assertions.assertTrue(mrrPickslips.get(0).visiting(), "Should have visiting status"),
                () -> Assertions.assertTrue(mrrPickslips.get(0).parked(), "Should have parked status")
                );

        Assertions.assertNotNull(pickslipQueues.getPickslipByRequestId("e688d594-379b-456d-845c-e68c14e613d8"), "Should exist");
        Assertions.assertEquals(1, pickslipQueues.getVisitorsForStack("MRR-SP").size(), "Should be visiting MRR stack");
        Assertions.assertEquals(0, pickslipQueues.getVisitorsForStack("MITCHELL-SP").size(), "Should be no visitors");
    }

}
