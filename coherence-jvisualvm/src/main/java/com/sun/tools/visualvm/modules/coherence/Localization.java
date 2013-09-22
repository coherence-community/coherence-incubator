/*
 * File: Localization.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
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

package com.sun.tools.visualvm.modules.coherence;

import org.openide.util.NbBundle;

/**
 * Holds methods for localization of messages.  The default English/US
 * messages are store in Bundle.properties.  The localization of this
 * is done using by specifying properties as such:
 * <br>
 * <ul>
 *   <li>Bundle_[language]_[country].properties o</li>
 *   <li>Bundle_[language].properties<li>
 * </li>
 * </ul>
 * For example:<br>
 * <ul>
 *   <li>Bundle_fr.properties - French</li>
 *   <li>Bundle_fr_CA.properties - French Canadian</li>
 *   <li>Bundle_ja.properties - Japanese</li>
 * </ul>
 *
 * @author Tim Middleton
 */
public class Localization
{
    /**
     * Return a localized version of text obtained from Bundle.properties by
     * default or localized bundle as described above.
     * <br>
     * Example:
     * <pre>
     * String sLabel = Localization.getLocalText("LBL_cluster_name");
     * </pre>
     * Bundle.properties should contain a line with the text:<br>
     * <pre>
     * LBL_cluster_name=Cluster Name:
     * </pre>
     *
     * @param sKey the key to obtain the localization for
     *
     * @return the localized message
     */
    public static String getLocalText(String sKey)
    {
        return NbBundle.getMessage(Localization.class, sKey);
    }


    /**
     * Return a localized version of text obtained from Bundle.properties by
     * default or localized bundle as described above.
     *
     * Example:
     * <pre>
     * String sLabel = Localization.getLocalText("MSG_file_not_found", new String[] {"tim.txt"});
     * </pre>
     * Bundle.properties should contain a line with the text:<br>
     * <pre>
     * MSG_file_not_found=The file {0} was not found.
     * </pre>
     *
     * @param sKey     the key to obtain the localization for
     * @param asParams the array of parameters to substitue
     *
     * @return the localized message
     */
    public static String getLocalText(String sKey,
                                      String asParams[])
    {
        return NbBundle.getMessage(Localization.class, sKey, asParams);
    }
}
