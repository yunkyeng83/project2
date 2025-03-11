package bio.mapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;
@Mapper
public interface BioInvStockMapper {
    List<Map<String, Object>> getReceivedAndShippedQuantities();
}