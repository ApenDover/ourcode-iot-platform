package ts.andrey.eventcollector.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.eventcollector.metrics.GlobalMetrics;
import ts.andrey.eventcollector.service.component.CollectorFacade;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CollectorFacadeTest {

    @Mock
    DeviceEventServiceImpl deviceEventServiceImpl;

    @Mock
    DeviceServiceImpl deviceServiceImpl;

    @Mock
    GlobalMetrics globalMetrics;

    @InjectMocks
    CollectorFacade collectorService;

    @Test
    void collectShouldLogErrorWhenExceptionOccurs() {
        // GIVEN
        var events = DummyTDF.deviceEvent.getList(2);
        doThrow(new RuntimeException("test exception")).when(deviceServiceImpl).sendUniqueDeviceids(anyList());

        // WHEN
        Assertions.assertThrows(RuntimeException.class, () -> collectorService.collect(events));

        // THEN
        verifyNoInteractions(deviceEventServiceImpl);
    }

    @Test
    void collectShouldNotFailOnEmptyInput() {
        // WHEN
        collectorService.collect(Collections.emptyList());

        // THEN
        verifyNoInteractions(deviceServiceImpl);
        verifyNoInteractions(deviceEventServiceImpl);
    }

}
