package com.juke.migration.user.upload;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.util.FileUtil;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HeaderCache {

    private final Unmarshaller unmarshaller = createUnmarshaller();

    @Autowired
    private FileUtil fileUtil;

    private final LoadingCache<UserEbook, Map<String, String>> headerCache = CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<UserEbook, Map<String, String>>() {
        @Override
        public Map<String, String> load(UserEbook key) throws Exception {
            final Root root = (Root) unmarshaller.unmarshal(fileUtil.getHeader(key));
            final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            for (Header header : root.getHeaders()) {
                builder.put(header.getKey(), header.getValue());
            }
            return builder.build();
        }
    });

    private Unmarshaller createUnmarshaller(){
        try {
            final JAXBContext jc = JAXBContext.newInstance(Root.class);
            return jc.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getHeaders(final UserEbook ebook) {
        return headerCache.getUnchecked(ebook);
    }

}
