/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.eigenbase.sql.fun;

import java.math.*;

import org.eigenbase.sql.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.sql.validate.*;


/**
 * Base class for binary operators such as addition, subtraction, and
 * multiplication which are monotonic for the patterns <code>m op c</code> and
 * <code>c op m</code> where m is any monotonic expression and c is a constant.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class SqlMonotonicBinaryOperator
    extends SqlBinaryOperator
{
    //~ Constructors -----------------------------------------------------------

    public SqlMonotonicBinaryOperator(
        String name,
        SqlKind kind,
        int prec,
        boolean isLeftAssoc,
        SqlReturnTypeInference returnTypeInference,
        SqlOperandTypeInference operandTypeInference,
        SqlOperandTypeChecker operandTypeChecker)
    {
        super(
            name,
            kind,
            prec,
            isLeftAssoc,
            returnTypeInference,
            operandTypeInference,
            operandTypeChecker);
    }

    //~ Methods ----------------------------------------------------------------

    public SqlMonotonicity getMonotonicity(
        SqlCall call,
        SqlValidatorScope scope)
    {
        final SqlMonotonicity mono0 = scope.getMonotonicity(call.operands[0]);
        final SqlMonotonicity mono1 = scope.getMonotonicity(call.operands[1]);

        // constant <op> constant --> constant
        if ((mono1 == SqlMonotonicity.Constant)
            && (mono0 == SqlMonotonicity.Constant))
        {
            return SqlMonotonicity.Constant;
        }

        // monotonic <op> constant
        if (mono1 == SqlMonotonicity.Constant) {
            // mono0 + constant --> mono0
            // mono0 - constant --> mono0
            if (getName().equals("-")
                || getName().equals("+"))
            {
                return mono0;
            }
            assert getName().equals("*");
            if (call.operands[1] instanceof SqlLiteral) {
                SqlLiteral literal = (SqlLiteral) call.operands[1];
                switch (literal.signum()) {
                case -1:

                    // mono0 * negative constant --> reverse mono0
                    return mono0.reverse();
                case 0:

                    // mono0 * 0 --> constant (zero)
                    return SqlMonotonicity.Constant;
                default:

                    // mono0 * positiove constant --> mono0
                    return mono0;
                }
            }
            return mono0;
        }

        // constant <op> mono
        if (mono0 == SqlMonotonicity.Constant) {
            if (getName().equals("-")) {
                // constant - mono1 --> reverse mono1
                return mono1.reverse();
            }
            if (getName().equals("+")) {
                // constant + mono1 --> mono1
                return mono1;
            }
            assert getName().equals("*");
            if (call.operands[0] instanceof SqlLiteral) {
                SqlLiteral literal = (SqlLiteral) call.operands[0];
                switch (literal.signum()) {
                case -1:

                    // negative constant * mono1 --> reverse mono1
                    return mono1.reverse();
                case 0:

                    // 0 * mono1 --> constant (zero)
                    return SqlMonotonicity.Constant;
                default:

                    // positive constant * mono1 --> mono1
                    return mono1;
                }
            }
        }

        // strictly asc + strictly asc --> strictly asc
        //   e.g. 2 * orderid + 3 * orderid
        //     is strictly increasing if orderid is strictly increasing
        // asc + asc --> asc
        //   e.g. 2 * orderid + 3 * orderid
        //     is increasing if orderid is increasing
        // asc + desc --> not monotonic
        //   e.g. 2 * orderid + (-3 * orderid) is not monotonic

        if (getName().equals("+")) {
            if (mono0 == mono1) {
                return mono0;
            } else if (mono0.unstrict() == mono1.unstrict()) {
                return mono0.unstrict();
            } else {
                return SqlMonotonicity.NotMonotonic;
            }
        }
        if (getName().equals("-")) {
            if (mono0 == mono1.reverse()) {
                return mono0;
            } else if (mono0.unstrict() == mono1.reverse().unstrict()) {
                return mono0.unstrict();
            } else {
                return SqlMonotonicity.NotMonotonic;
            }
        }
        if (getName().equals("*")) {
            return SqlMonotonicity.NotMonotonic;
        }

        return super.getMonotonicity(call, scope);
    }
}

// End SqlMonotonicBinaryOperator.java
