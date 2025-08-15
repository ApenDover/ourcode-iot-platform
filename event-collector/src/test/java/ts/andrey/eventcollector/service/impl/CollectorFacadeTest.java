package ts.andrey.eventcollector.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.eventcollector.service.component.CollectorFacade;
import ts.andrey.eventcollector.service.component.SimpleCache;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CollectorFacadeTest {

    @Mock
    DeviceEventServiceImpl deviceEventServiceImpl;
    @Mock
    DeviceServiceImpl deviceServiceImpl;
    @Mock
    SimpleCache simpleCache;
    @InjectMocks
    CollectorFacade collectorService;

    @Test
    void collectShouldProcessOnlyValidEvents() {
        // GIVEN
        final var validEvent = DummyTDF.deviceEvent.getDefault();
        final var invalidEvent = DummyTDF.deviceEvent.getInvalid();
        final var events = List.of(validEvent, invalidEvent);

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceServiceImpl).sendUniqueDeviceids(List.of(validEvent));
        verify(deviceEventServiceImpl).saveEvents(List.of(validEvent));
    }

    @Test
    void collectShouldLogErrorWhenExceptionOccurs() {
        // GIVEN
        var events = DummyTDF.deviceEvent.getList(2);
        doThrow(new RuntimeException("test exception")).when(deviceServiceImpl).sendUniqueDeviceids(anyList());

        // WHEN
        collectorService.collect(events);

        // THEN
        verifyNoInteractions(deviceEventServiceImpl);
        verifyNoInteractions(simpleCache);
    }

    @Test
    void collectShouldNotFailOnEmptyInput() {
        // WHEN
        collectorService.collect(Collections.emptyList());

        // THEN
        verifyNoInteractions(deviceServiceImpl);
        verifyNoInteractions(deviceEventServiceImpl);
        verifyNoInteractions(simpleCache);
    }

}
