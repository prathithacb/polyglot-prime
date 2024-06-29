package lib.aide.resource;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;

import lib.aide.paths.Paths;

public class VfsResources
        implements ResourcesSupplier<VfsResources.VfsFileObjectProvenance, String, Resource<? extends Nature, ?>> {
    public record VfsFileObjectProvenance(URI uri, FileObject fileObject) implements Provenance {
    }

    private final ResourceFactory rf;
    private final FileObjectPathComponetsSupplier fopcs = new FileObjectPathComponetsSupplier();
    private final URI identity;
    private final FileObject rootVfsFO;

    public VfsResources(final ResourceFactory rf, final URI identity, final FileObject rootVfsFO) throws Exception {
        this.rf = rf;
        this.identity = identity;
        this.rootVfsFO = rootVfsFO;
    }

    @Override
    public URI identity() {
        return identity;
    }

    @Override
    public Paths<String, ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>> paths() {
        Paths<String, ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>> paths;
        final var payloadRoot = new ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>(
                new VfsFileObjectProvenance(rootVfsFO.getURI(), rootVfsFO),
                EmptyResource.SINGLETON);
        paths = new Paths<String, ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>>(
                payloadRoot,
                fopcs);
        for (var resourceProvenance : resources()) {
            paths.populate(resourceProvenance);
        }
        return paths;
    }

    @Override
    public List<ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>> resources() {
        var resourceList = new ArrayList<ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>>();
        try {
            walkFileSystem(rootVfsFO, resourceList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resourceList;
    }

    private void walkFileSystem(FileObject fileObject,
            List<ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>> resourceList)
            throws Exception {
        if (fileObject.isFolder()) {
            for (var child : fileObject.getChildren()) {
                walkFileSystem(child, resourceList);
            }
        } else {
            var resource = rf.resourceFromSuffix(fileObject.getName().getBaseName(), () -> {
                try {
                    // TODO: this should really check the nature first not just always return text
                    // content
                    return fileObject.getContent().getString(Charset.defaultCharset());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, Optional.empty()).orElse(new ExceptionResource(new RuntimeException("Unsupported resource type")));
            var provenance = new VfsFileObjectProvenance(fileObject.getURI(), fileObject);
            resourceList.add(new ResourceProvenance<>(provenance, resource));
        }
    }

    class FileObjectPathComponetsSupplier implements
            Paths.PayloadComponentsSupplier<String, ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>>> {
        @Override
        public List<String> components(
                ResourceProvenance<VfsFileObjectProvenance, Resource<? extends Nature, ?>> payload) {
            return components(payload.provenance().fileObject().toString());
        }

        @Override
        public List<String> components(String path) {
            return List.of(path.split("/"));
        }

        @Override
        public String assemble(List<String> components) {
            return String.join("/", components);
        }
    }
}
