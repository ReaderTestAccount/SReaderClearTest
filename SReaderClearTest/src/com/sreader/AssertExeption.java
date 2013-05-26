package com.sreader;

import android.util.Log;

public class AssertExeption extends RuntimeException
{
	private static final long serialVersionUID = 8640734202495771411L;

	private AssertExeption(String errorDescription)
	{
		super(errorDescription);
	}

	private AssertExeption(String errorDescription, Throwable cause)
	{
		super(errorDescription, cause);
	}

	public static void _assert(boolean expr, String errorDescription)
	{
		if (Config.ASSERT_ENABLED)
			if (!expr) {
				Log.e("SReader assert", errorDescription);
				throw new AssertExeption(errorDescription);
			}
	}

	public static void _assert(boolean expr, String errorDescription, Throwable cause)
	{
		if (Config.ASSERT_ENABLED)
			if (!expr) {
				AssertExeption._assert(cause != null, "Get null as cause throwable. errorDescription: " + errorDescription);
				cause.printStackTrace();
				Log.e("SReader assert", errorDescription);
				throw new AssertExeption(errorDescription, cause);
			}
	}

	public static void _assertNull(Object o, String additionalDescription)
	{
		if (Config.ASSERT_ENABLED)
			if (null == o) {
				String message;
				final String staticMessage = "Unexpected null reference.";
				if (null != additionalDescription) {
					StringBuilder sb = new StringBuilder(staticMessage).append(" Details: ").append(additionalDescription);
					message = sb.toString();
				} else
					message = staticMessage;

				Log.e("SReader assert", message);
				throw new AssertExeption(message);
			}
	}

	public static void _assertIfNegative(Number val, String additionalDescription)
	{
		if (Config.ASSERT_ENABLED) {
			if (null == val) {
				throw new AssertExeption("Given number value is null");
			} else {
				if (val.doubleValue() < 0) {
					String message;
					final String staticMessage = "Given value is less than 0;";
					if (null != additionalDescription) {
						StringBuilder sb = new StringBuilder(staticMessage).append(" Details: ").append(additionalDescription);
						message = sb.toString();
					} else
						message = staticMessage;

					Log.e("SReader assert", message);
					throw new AssertExeption(message);
				}
			}
		}
	}

	public static void _assertFalseSwitchDefault()
	{
		if (Config.ASSERT_ENABLED) {
			final String staticMessage = "Logic error: unexpected value don't perform by switch.";
			Log.e("SReader assert", staticMessage);
			throw new AssertExeption(staticMessage);
		}
	}

	public static void _assertFalseSwitchDefault(String additionalDescription)
	{
		if (Config.ASSERT_ENABLED) {
			String message;
			final String staticMessage = "Logic error: unexpected value don't perform by switch.";
			if (null != additionalDescription) {
				StringBuilder sb = new StringBuilder(staticMessage).append(" Details: ").append(additionalDescription);
				message = sb.toString();
			} else
				message = staticMessage;

			Log.e("SReader assert", message);
			throw new AssertExeption(message);
		}
	}

	public static void _assertParamNull(Object obj, String varName)
	{
		final String prefix = "wrong input argument - ";
		final String postfix = " can't be null";

		if (Config.ASSERT_ENABLED)
			if (null == obj) {
				StringBuilder sb = new StringBuilder();
				sb.append(prefix);
				if (null != varName)
					sb.append(varName);
				sb.append(postfix);

				String errorDescription = sb.toString();
				Log.e("SReader assert", errorDescription);
				throw new AssertExeption(sb.toString());
			}
	}

	public static void _assertParamNull(Object[] objs, String[] varNames)
	{
		final String prefix = "wrong input arguments - ";
		final String postfix = " can't be null";

		if (Config.ASSERT_ENABLED) {
			AssertExeption._assert(null != objs, "Assert called with null pinter instead of parameter array");
			boolean haveNullObj = false;
			final int len = objs.length;
			for (int i = 0; i < len; i++)
				if (null == objs[i]) {
					haveNullObj = true;
					break;
				}

			if (haveNullObj) {
				StringBuilder sb = new StringBuilder();
				sb.append(prefix);
				if (null != varNames)
					for (int i = 0; i < varNames.length; i++) {
						sb.append(varNames[i]);
						if (i < varNames.length - 1)
							sb.append(", ");
					}
				sb.append(postfix);

				String errorDescription = sb.toString();
				Log.e("SReader assert", errorDescription);
				throw new AssertExeption(errorDescription);
			}
		}
	}
}
