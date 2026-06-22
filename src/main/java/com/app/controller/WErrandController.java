package com.app.controller;

import com.app.common.Result;
import com.app.service.WErrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/app/errand")
public class WErrandController {

    @Autowired
    private WErrandService errandService;

    @GetMapping("/orders")
    public Result<Map<String, Object>> getOrders(
            @RequestParam(value = "status", defaultValue = "unaccepted") String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> result = errandService.getOrders(status, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/check-runner")
    public Result<Map<String, Object>> checkRunner(@RequestParam("userId") Integer userId) {
        try {
            boolean isRunner = errandService.isErrandRunner(userId);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("isRunner", isRunner);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/order/{orderId}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable("orderId") Integer orderId) {
        try {
            Map<String, Object> result = errandService.getOrderDetail(orderId);
            if (result == null) {
                return Result.error("订单不存在");
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/accept")
    public Result<Object> acceptOrder(@RequestParam("postId") Integer postId,
                                      @RequestParam("acceptorId") Integer acceptorId) {
        try {
            if (postId == null || acceptorId == null) {
                return Result.error("参数不能为空");
            }
            boolean success = errandService.acceptOrder(postId, acceptorId);
            if (success) {
                return Result.success("接单成功", null);
            } else {
                return Result.error("接单失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("接单失败：" + e.getMessage());
        }
    }

    @GetMapping("/super-admin-id")
    public Result<Map<String, Object>> getSuperAdminId() {
        try {
            Integer adminId = errandService.getSuperAdminId();
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("adminId", adminId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/cancel-publish")
    public Result<Object> cancelPublish(@RequestParam("postId") Integer postId,
                                        @RequestParam("userId") Integer userId) {
        try {
            if (postId == null || userId == null) {
                return Result.error("参数不能为空");
            }
            boolean success = errandService.cancelPublish(postId, userId);
            if (success) {
                return Result.success("取消发布成功", null);
            } else {
                return Result.error("取消发布失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("取消发布失败：" + e.getMessage());
        }
    }

    @PostMapping("/complete-order")
    public Result<Object> completeOrder(@RequestParam("postId") Integer postId,
                                       @RequestParam("acceptorId") Integer acceptorId) {
        try {
            if (postId == null || acceptorId == null) {
                return Result.error("参数不能为空");
            }
            boolean success = errandService.completeOrder(postId, acceptorId);
            if (success) {
                return Result.success("完成订单成功", null);
            } else {
                return Result.error("完成订单失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("完成订单失败：" + e.getMessage());
        }
    }
}
