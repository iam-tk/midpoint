package com.evolveum.midpoint.provisioning.impl.shadowcache;

import static java.util.Objects.requireNonNull;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadowcache.ShadowAcquisition.ResourceObjectSupplier;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Helps with the resource object adoption process (acquiring repo shadows, shadow completion).
 *
 * Currently it delegates these activities to dedicated classes: {@link ShadowAcquisition}, {@link ReturnedObjectConstruction}.
 */
@Experimental
@Component
class AdoptionHelper {

    @Autowired private CommonBeans commonBeans;

    /**
     * Acquires repository shadow for a provided resource object. The repository shadow is located or created.
     * In case that the shadow is created, all additional ceremonies for a new shadow is done, e.g. invoking
     * change notifications (discovery).
     *
     * Returned shadow is NOT guaranteed to have all the attributes aligned and updated. That is only possible after
     * completeShadow(). But maybe, this method can later invoke completeShadow() and do all the necessary stuff?
     *
     * It may look like this method would rather belong to ShadowManager. But it does NOT. It does too much stuff
     * (e.g. change notification).
     */
    public @NotNull PrismObject<ShadowType> acquireRepoShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        PrismProperty<?> primaryIdentifier = requireNonNull(
                ProvisioningUtil.getSingleValuedPrimaryIdentifier(resourceObject),
                () -> "No primary identifier value in " + ShadowUtil.shortDumpShadow(resourceObject));
        QName objectClass = requireNonNull(
                resourceObject.asObjectable().getObjectClass(),
                () -> "No object class in " + ShadowUtil.shortDumpShadow(resourceObject));

        return new ShadowAcquisition(ctx, primaryIdentifier, objectClass, () -> resourceObject, commonBeans)
                .execute(result);
    }

    public @NotNull PrismObject<ShadowType> acquireRepoShadow(ProvisioningContext ctx, PrismProperty<?> primaryIdentifier,
            QName objectClass, ResourceObjectSupplier resourceObjectSupplier, OperationResult result) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, GenericConnectorException,
            ExpressionEvaluationException, EncryptionException, SecurityViolationException {

        return new ShadowAcquisition(ctx, primaryIdentifier, objectClass, resourceObjectSupplier, commonBeans)
                .execute(result);
    }

    /**
     * TODO improve the description
     *
     * Make sure that the repo shadow is complete, e.g. that all the mandatory fields
     * are filled (e.g name, resourceRef, ...) Also transforms the shadow with
     * respect to simulated capabilities. Also shadowRefs are added to associations.
     */
    @NotNull PrismObject<ShadowType> constructReturnedObject(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, PrismObject<ShadowType> resourceObject,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        return ReturnedObjectConstruction.create(ctx, repoShadow, resourceObject, commonBeans)
                .construct(parentResult);
    }

}
