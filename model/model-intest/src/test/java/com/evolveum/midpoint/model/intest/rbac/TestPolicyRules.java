/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.rbac;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules extends AbstractInitializedModelIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/rbac/policy-rules");

    private static final File POLICY_SKIPPER_LICENSE_FILE = new File(TEST_DIR, "policy-skipper-license.xml");
    private static final String POLICY_SKIPPER_LICENSE_OID = "25f26c46-427f-11ef-9666-f352a031a80e";
    private static final String POLICY_SKIPPER_LICENSE_NAME = "Skipper license";

    private static final File ROLE_SKIPPER_FILE = new File(TEST_DIR, "role-skipper.xml");
    private static final String ROLE_SKIPPER_OID = "bbfe9846-427e-11ef-a31c-53388393ba50";
    private static final String ROLE_SKIPPER_NAME = "Skipper";

    // Business roles that includes Skipper
    private static final File ROLE_NAVY_CAPTAIN_FILE = new File(TEST_DIR, "role-navy-captain.xml");
    private static final String ROLE_NAVY_CAPTAIN_OID = "b577476c-438b-11ef-b695-030d5a076b98";
    private static final String ROLE_NAVY_CAPTAIN_NAME = "Navy Captain";

    // Role that includes Skipper license
    private static final File ROLE_NAVAL_ACADEMY_GRADUATE_FILE = new File(TEST_DIR, "role-naval-academy-graduate.xml");
    private static final String ROLE_NAVAL_ACADEMY_GRADUATE_OID = "31cfa124-438c-11ef-865e-0bfb88c1246d";
    private static final String ROLE_NAVAL_ACADEMY_GRADUATE_NAME = "Naval academy graduate";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        if (isNativeRepository()) {
            repoAddObjectFromFile(POLICY_SKIPPER_LICENSE_FILE, initResult);
            repoAddObjectFromFile(ROLE_SKIPPER_FILE, initResult);
            repoAddObjectFromFile(ROLE_NAVY_CAPTAIN_FILE, initResult);
            repoAddObjectFromFile(ROLE_NAVAL_ACADEMY_GRADUATE_FILE, initResult);
        }
    }

    @Test
    public void test110DirectRequirementSkipperFail() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        try {
            // Jack jas no skipper license, it should fail
            assignRole(USER_JACK_OID, ROLE_SKIPPER_OID, task, result);

            fail("Expected policy violation after adding skipper role, but it went well");
        } catch (PolicyViolationException e) {
            System.out.println("Got expected exception: " + e + ": " + e.getMessage());
            assertMessage(e, "Policy requirement not met: role \"Skipper\" requires policy \"Skipper license\"");
            result.computeStatus();
            assertFailure(result);
        }

        display("User after", getUser(USER_JACK_OID));

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    @Test
    public void test120DirectRequirementSkipperSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE_OID, task, result);

        // Jack has skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_SKIPPER_OID, task, result);

        unassignRole(USER_JACK_OID, ROLE_SKIPPER_OID, task, result);

        unassignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE_OID, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  indirect, role skipper in business role navy captain
    @Test
    public void test130IndirectRequirementNavyCaptainFail() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        try {
            // Jack jas no skipper license, it should fail
            assignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN_OID, task, result);

            fail("Expected policy violation after adding navy captain role, but it went well");
        } catch (PolicyViolationException e) {
            System.out.println("Got expected exception: " + e + ": " + e.getMessage());
            assertMessage(e, "Policy requirement not met: role \"Skipper\" (Navy captain -> Skipper) requires policy \"Skipper license\"");
            result.computeStatus();
            assertFailure(result);
        }

        display("User after", getUser(USER_JACK_OID));

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  indirect, role skipper in business role navy captain
    @Test
    public void test140IndirectRequirementNavyCaptainSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        assignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE_OID, task, result);

        // Jack has skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN_OID, task, result);

        unassignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN_OID, task, result);

        unassignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE_OID, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  indirect, skipper license in graduate role
    @Test
    public void test150IndirectRequirementGraduateSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        assignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE_OID, task, result);

        // Jack has (indirect) skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_SKIPPER_OID, task, result);

        unassignRole(USER_JACK_OID, ROLE_SKIPPER_OID, task, result);

        unassignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE_OID, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  double indirect, role skipper in business role navy captain, skipper license in graduate role
    @Test
    public void test160IndirectRequirementNavyCaptainGraduateSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        assignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE_OID, task, result);

        // Jack has (indirect) skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN_OID, task, result);

        unassignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN_OID, task, result);

        unassignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE_OID, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }


    // TODO: requirement: assign both license and skipper in one operation

    // TODO: requirement: unassign both license and skipper in one operation

    // TODO: requirement: business roles that contains both license and role skipper



}
