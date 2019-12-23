package com.xm.api_mall.controller;

import com.xm.api_mall.component.PlatformContext;
import com.xm.comment.annotation.LoginUser;
import com.xm.comment.module.user.feign.UserFeignClient;
import com.xm.comment.response.Msg;
import com.xm.comment.response.R;
import com.xm.comment_serialize.module.mall.bo.ProductIndexBo;
import com.xm.comment_serialize.module.mall.entity.SmProductEntity;
import com.xm.comment_serialize.module.mall.form.GetProductSaleInfoForm;
import com.xm.comment_serialize.module.mall.form.ProductDetailForm;
import com.xm.comment_serialize.module.mall.form.ProductListForm;
import com.xm.comment_utils.mybatis.PageBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private PlatformContext productContext;
    @Autowired
    private UserFeignClient userFeignClient;

    @Resource(name = "myExecutor")
    private ThreadPoolTaskExecutor executor;
    /**
     * 推荐商品列表
     * @return
     */
    @PostMapping("/list")
    public Msg<Object> getProductList(@RequestBody @Valid ProductListForm productListForm, BindingResult bindingResult, @LoginUser(necessary = false) Integer userId) throws Exception {
        return R.sucess(
                productContext
                .platformType(productListForm.getPlatformType())
                .listType(productListForm.getListType())
                .invoke(userId,productListForm));
    }

    /**
     * 获取商品详情
     * @return
     */
    @GetMapping("/detail")
    public Msg<SmProductEntity> getProductDetail(@Valid ProductDetailForm productDetailForm, BindingResult bindingResult, @LoginUser(necessary = false) Integer userId) throws Exception {
//        userFeignClient.addProductHistory(userId,productDetailForm.getPlatformType(),productDetailForm.getGoodsId());
        return R.sucess(productContext
                .platformType(productDetailForm.getPlatformType())
                .getService()
                .detail(productDetailForm.getGoodsId()));
    }

    /**
     * 批量获取商品详情
     * @return
     */
    @GetMapping("/details")
    public Msg<List<SmProductEntity>> getProductDetails(Integer platformType,@RequestParam("goodsIds") List<String> goodsIds) throws Exception {
        return R.sucess(productContext
                .platformType(platformType)
                .getService()
                .details(goodsIds));
    }

    /**
     * 批量获取商品详情
     * @return
     */
    @PostMapping("/details")
    public Msg<List<SmProductEntity>> getProductDetails(@RequestBody List<ProductIndexBo> productIndexBos) throws Exception {
        Map<Integer, List<ProductIndexBo>> group = productIndexBos.stream().collect(Collectors.groupingBy(ProductIndexBo::getPlatformType));
        List<Future<List<SmProductEntity>>> futures = new ArrayList<>();
        for (Map.Entry<Integer, List<ProductIndexBo>> integerListEntry : group.entrySet()) {
            Future<List<SmProductEntity>> listFuture = executor.submit(new Callable<List<SmProductEntity>>() {
                @Override
                public List<SmProductEntity> call() throws Exception {
                    return productContext
                            .platformType(integerListEntry.getKey())
                            .getService()
                            .details(integerListEntry.getValue().stream().map(ProductIndexBo::getGoodsId).collect(Collectors.toList()));
                }
            });
            futures.add(listFuture);
        }
        //合并结果
        List<SmProductEntity> result = new ArrayList<>();
        futures.forEach(o ->{
            try {
                List<SmProductEntity> smProductEntities = o.get();
                result.addAll(smProductEntities);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        //按原来顺序排序
//        List<SmProductEntity> sort = new ArrayList<>();
//        productIndexBos.stream().forEach(o ->{
//            sort.add(result.stream().filter(j ->{
//                return o.getGoodsId().equals(j.getGoodsId());
//            }).findFirst().get());
//        });

        return R.sucess(result);
    }

    @GetMapping("/sale")
    public Msg getProductSaleInfo(@LoginUser Integer userId,@Valid GetProductSaleInfoForm productSaleInfoForm) throws Exception {
        if(userId.equals(productSaleInfoForm.getShareUserId()))
            productSaleInfoForm.setShareUserId(null);
        return R.sucess(productContext
                .platformType(productSaleInfoForm.getPlatformType())
                .getService()
                .saleInfo(
                        userId,
                        productSaleInfoForm.getAppType(),
                        productSaleInfoForm.getShareUserId(),
                        productSaleInfoForm.getGoodsId()));
    }


}
