package au.gov.nla.pickslip.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HomeControllerTests {

    @Test
    void getDeweyish() {

        HomeController homeController = new HomeController();

        Assertions.assertEquals(homeController.getDeweyish("NL 582.1309945 G148"), 582.1309945, 0.000001);
        Assertions.assertEquals(homeController.getDeweyish("N 2019-1537"), 2019.1537, 0.0001);
        Assertions.assertEquals(homeController.getDeweyish("ORAL TRC 3800"), 0);
        Assertions.assertEquals(homeController.getDeweyish("Ephemera (Trade catalogues)"), 0);
        Assertions.assertEquals(homeController.getDeweyish("mfm X 650/reels 5,704-5,790."), 0);
    }

}
