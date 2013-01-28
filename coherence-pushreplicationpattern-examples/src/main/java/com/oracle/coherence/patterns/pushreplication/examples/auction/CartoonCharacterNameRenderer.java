/*
 * File: CartoonCharacterNameRenderer.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.patterns.pushreplication.examples.auction;

/**
 * A {@link CustomerReferenceRenderer} that returns a cartoon characters name.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CartoonCharacterNameRenderer implements CustomerReferenceRenderer
{
    private static final String[][] CARTOON_CHARACTER_NAMES =
    {
        {"Daffy Duck", "Bugs Bunny", "Elmer Fudd", "Foghorn Leghorn", "Wile E. Coyote", "Sylvester", "Porky Pig",
         "Yosemite Sam", "Speedy Gonzales", "Tazmanian Devil"},
        {"Donald Duck", "Mickey Mouse", "Goofy", "Pluto", "Cinderella", "Snow White", "Bambi", "Dumbo", "Tinker Bell",
         "Peter Pan"}
    };


    /**
     * {@inheritDoc}
     */
    public String getDisplayName(CustomerReference customerReference)
    {
        if (customerReference == null)
        {
            return null;
        }
        else if (customerReference.getSite().equalsIgnoreCase("Site1") && customerReference.getId() >= 0
                 && customerReference.getId() < CARTOON_CHARACTER_NAMES[0].length)
        {
            return CARTOON_CHARACTER_NAMES[0][customerReference.getId()];

        }
        else if (customerReference.getSite().equalsIgnoreCase("Site2") && customerReference.getId() >= 0
                 && customerReference.getId() < CARTOON_CHARACTER_NAMES[1].length)
        {
            return CARTOON_CHARACTER_NAMES[1][customerReference.getId()];

        }
        else
        {
            return String.format("(unknown #%d)", customerReference.getId());
        }
    }
}
