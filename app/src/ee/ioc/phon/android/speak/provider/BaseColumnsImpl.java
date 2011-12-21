package ee.ioc.phon.android.speak.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class BaseColumnsImpl implements BaseColumns {

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ee.ioc.phon.android.speak";

	public static Uri makeContentUri(String name) {
		return Uri.parse("content://" + AppsContentProvider.AUTHORITY + "/" + name);
	}

}