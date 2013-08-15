/*
 * File: SerializableExpressionHelper.java
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

package com.oracle.coherence.common.expression;


import com.tangosol.coherence.config.ParameterMacroExpression;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.io.pof.PortableObject;

/**
 * The SerializableExpressionHelper converts non-serializable Expressions to serializable Expressions
 *
 * @author Paul Mackin
 */
public class SerializableExpressionHelper
{
    /**
     * Ensure the expression is Serializable
     *
     * @param expression  the Expression to be ensured.
     *
     * @return  the serializable Expresssion
     */
    public static Expression ensureSerializable(Expression expression)
    {
        if (expression instanceof PortableObject)
        {
            return expression;
        }

        Expression newExpression = expression;

        if (expression instanceof LiteralExpression)
        {
            newExpression = new SerializableLiteralExpression(expression.evaluate(new NullParameterResolver()));
        }
        else if (expression instanceof ParameterMacroExpression)
        {
            String   sExpression = null;
            String   macro       = expression.toString();
            String[] first       = macro.split("type=class ");
            String[] second      = first[1].split(", expression=");

            String   sType       = second[0];

            if (second.length > 1)
            {
                sExpression = second[1].substring(0, second[1].length() - 1);
            }

            try
            {
                newExpression = new SerializableParameterMacroExpression(sExpression, Class.forName(sType));
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalStateException("Expression can not be created for class " + sType, e);
            }
        }

        return newExpression;
    }
}

