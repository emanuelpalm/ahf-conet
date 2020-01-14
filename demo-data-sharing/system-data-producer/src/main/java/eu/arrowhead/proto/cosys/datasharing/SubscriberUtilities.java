package eu.arrowhead.proto.cosys.datasharing;

import eu.arrowhead.proto.cosys.datasharing.DataProviderConstants;
import eu.arrowhead.common.dto.shared.SubscriptionRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;

public class SubscriberUtilities {

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public static SubscriptionRequestDTO createSubscriptionRequestDTO( final String eventType, final SystemRequestDTO subscriber, final String notificationUri) {

        final SubscriptionRequestDTO subscription = new SubscriptionRequestDTO(
                eventType.toUpperCase(),
                subscriber,
                null,
                DataProviderConstants.PROVIDER_URI + "/" + notificationUri,
                false,
                null,
                null,
                null);

        return subscription;
    }

}
